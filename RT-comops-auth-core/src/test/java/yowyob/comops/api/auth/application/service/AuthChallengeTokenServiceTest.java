package yowyob.comops.api.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import yowyob.comops.api.auth.application.service.AuthChallengeTokenService.IssuedCaptchaChallenge;
import yowyob.comops.api.auth.application.service.AuthChallengeTokenService.IssuedCaptchaVerification;
import yowyob.comops.api.auth.application.service.AuthChallengeTokenService.IssuedOtpChallenge;
import yowyob.comops.api.auth.application.service.AuthChallengeTokenService.VerifiedOtpChallenge;
import yowyob.comops.api.kernel.config.JwtTokenService;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires pour AuthChallengeTokenService.
 *
 * Ce service gère deux types de "défis" de sécurité :
 *
 * 1. OTP (One-Time Password) — code à 6 chiffres envoyé par SMS ou email :
 *    issueOtp(purpose, channel, recipient, tenantId, userId)
 *    verifyOtp(token, code, expectedPurpose)
 *
 * 2. Captcha (addition de deux nombres) :
 *    issueCaptcha()              → émet le défi (ex: "23 + 17")
 *    verifyCaptcha(token, answer) → vérifie la réponse et émet un token de vérification
 *    verifyCaptchaVerification(token) → valide qu'un captcha a bien été résolu
 *
 * Analogie :
 *   - OTP = code SMS envoyé par ta banque pour confirmer un virement
 *   - Captcha = "prouve que tu n'es pas un robot" avant de t'inscrire
 */
class AuthChallengeTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private AuthChallengeTokenService challengeService;

    @BeforeEach
    void setUp() {
        SecurityRuntimeProperties props = new SecurityRuntimeProperties();
        props.getJwt().setAutoGenerateKeyPair(true);
        props.getJwt().setIssuer("yowyob-test");
        jwtTokenService = new JwtTokenService(props);
        challengeService = new AuthChallengeTokenService(jwtTokenService);
    }

    // =========================================================================
    // OTP — One-Time Password
    // =========================================================================

    @Nested
    @DisplayName("OTP — issueOtp() et verifyOtp()")
    class Otp {

        @Test
        @DisplayName("émet un token OTP non vide avec un code à 6 chiffres")
        void shouldIssueOtpWithSixDigitCode() {
            UUID tenantId = UUID.randomUUID();
            UUID userId   = UUID.randomUUID();

            IssuedOtpChallenge issued = challengeService.issueOtp(
                    "EMAIL_VERIFICATION", "EMAIL", "jean@example.com", tenantId, userId);

            assertThat(issued.token()).isNotBlank();
            assertThat(issued.codePreview())
                    .as("Le code OTP doit être à 6 chiffres")
                    .matches("\\d{6}");
            assertThat(issued.expiresInSeconds())
                    .as("TTL OTP : 5 minutes = 300 secondes")
                    .isEqualTo(300L);
        }

        @Test
        @DisplayName("un OTP valide est vérifié avec le bon code et le bon purpose")
        void shouldVerifyValidOtpWithCorrectCode() {
            UUID tenantId = UUID.randomUUID();
            UUID userId   = UUID.randomUUID();

            IssuedOtpChallenge issued = challengeService.issueOtp(
                    "LOGIN_MFA", "SMS", "+33600000000", tenantId, userId);

            Optional<VerifiedOtpChallenge> verified = challengeService.verifyOtp(
                    issued.token(), issued.codePreview(), "LOGIN_MFA");

            assertThat(verified).isPresent();
            assertThat(verified.get().purpose()).isEqualTo("LOGIN_MFA");
            assertThat(verified.get().channel()).isEqualTo("SMS");
            assertThat(verified.get().recipient()).isEqualTo("+33600000000");
            assertThat(verified.get().tenantId()).isEqualTo(tenantId);
            assertThat(verified.get().userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("retourne Optional.empty() si le code OTP est incorrect")
        void shouldRejectWrongCode() {
            IssuedOtpChallenge issued = challengeService.issueOtp(
                    "EMAIL_VERIFICATION", "EMAIL", "jean@example.com",
                    UUID.randomUUID(), UUID.randomUUID());

            // On utilise un faux code
            String wrongCode = issued.codePreview().equals("123456") ? "654321" : "123456";

            assertThat(challengeService.verifyOtp(issued.token(), wrongCode, "EMAIL_VERIFICATION"))
                    .isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() si le purpose attendu ne correspond pas")
        void shouldRejectWrongPurpose() {
            IssuedOtpChallenge issued = challengeService.issueOtp(
                    "EMAIL_VERIFICATION", "EMAIL", "jean@example.com",
                    UUID.randomUUID(), UUID.randomUUID());

            assertThat(challengeService.verifyOtp(issued.token(), issued.codePreview(), "LOGIN_MFA"))
                    .as("Le purpose 'LOGIN_MFA' ne correspond pas à 'EMAIL_VERIFICATION'")
                    .isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token OTP corrompu")
        void shouldRejectTamperedOtpToken() {
            IssuedOtpChallenge issued = challengeService.issueOtp(
                    "LOGIN_MFA", "SMS", "+33600000000", UUID.randomUUID(), UUID.randomUUID());

            String tampered = issued.token().substring(0, issued.token().length() - 8) + "FALSIFIE";

            assertThat(challengeService.verifyOtp(tampered, issued.codePreview(), "LOGIN_MFA"))
                    .isEmpty();
        }

        @Test
        @DisplayName("fonctionne sans tenantId ni userId (cas d'utilisation anonyme)")
        void shouldWorkWithoutTenantOrUserId() {
            IssuedOtpChallenge issued = challengeService.issueOtp(
                    "REGISTRATION", "EMAIL", "nouveau@example.com", null, null);

            assertThat(issued.token()).isNotBlank();

            Optional<VerifiedOtpChallenge> verified = challengeService.verifyOtp(
                    issued.token(), issued.codePreview(), "REGISTRATION");

            assertThat(verified).isPresent();
            assertThat(verified.get().tenantId()).isNull();
            assertThat(verified.get().userId()).isNull();
            assertThat(verified.get().recipient()).isEqualTo("nouveau@example.com");
        }
    }

    // =========================================================================
    // Captcha
    // =========================================================================

    @Nested
    @DisplayName("Captcha — issueCaptcha() et verifyCaptcha()")
    class Captcha {

        @Test
        @DisplayName("émet un captcha avec un prompt de type 'A + B'")
        void shouldIssueCaptchaWithArithmeticPrompt() {
            IssuedCaptchaChallenge issued = challengeService.issueCaptcha();

            assertThat(issued.token()).isNotBlank();
            assertThat(issued.prompt())
                    .as("Le prompt doit être de la forme 'A + B'")
                    .matches("\\d+ \\+ \\d+");
            assertThat(issued.answerPreview())
                    .as("La réponse preview doit être un entier")
                    .matches("\\d+");
            assertThat(issued.expiresInSeconds())
                    .as("TTL captcha : 5 minutes = 300 secondes")
                    .isEqualTo(300L);
        }

        @Test
        @DisplayName("un captcha est vérifié avec la bonne réponse")
        void shouldVerifyCaptchaWithCorrectAnswer() {
            IssuedCaptchaChallenge issued = challengeService.issueCaptcha();

            Optional<IssuedCaptchaVerification> verification =
                    challengeService.verifyCaptcha(issued.token(), issued.answerPreview());

            assertThat(verification).isPresent();
            assertThat(verification.get().token()).isNotBlank();
            assertThat(verification.get().expiresInSeconds())
                    .as("TTL vérification captcha : 10 minutes = 600 secondes")
                    .isEqualTo(600L);
        }

        @Test
        @DisplayName("retourne Optional.empty() si la réponse au captcha est incorrecte")
        void shouldRejectWrongCaptchaAnswer() {
            IssuedCaptchaChallenge issued = challengeService.issueCaptcha();

            // On donne une réponse complètement fausse
            String wrongAnswer = issued.answerPreview().equals("99") ? "0" : "99";

            assertThat(challengeService.verifyCaptcha(issued.token(), wrongAnswer))
                    .isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token captcha corrompu")
        void shouldRejectTamperedCaptchaToken() {
            IssuedCaptchaChallenge issued = challengeService.issueCaptcha();
            String tampered = issued.token().substring(0, issued.token().length() - 8) + "FALSIFIE";

            assertThat(challengeService.verifyCaptcha(tampered, issued.answerPreview()))
                    .isEmpty();
        }

        @Test
        @DisplayName("le token de vérification captcha est reconnu valide")
        void shouldAcceptValidCaptchaVerificationToken() {
            IssuedCaptchaChallenge issued = challengeService.issueCaptcha();

            IssuedCaptchaVerification verification =
                    challengeService.verifyCaptcha(issued.token(), issued.answerPreview()).orElseThrow();

            assertThat(challengeService.verifyCaptchaVerification(verification.token()))
                    .as("Le token de vérification captcha doit être reconnu valide")
                    .isTrue();
        }

        @Test
        @DisplayName("un token corrompu ou null est refusé comme token de vérification captcha")
        void shouldRejectInvalidCaptchaVerificationToken() {
            assertThat(challengeService.verifyCaptchaVerification(null)).isFalse();
            assertThat(challengeService.verifyCaptchaVerification("token.invalide")).isFalse();
        }
    }
}
