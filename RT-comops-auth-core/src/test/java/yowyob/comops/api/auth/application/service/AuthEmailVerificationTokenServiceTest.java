package yowyob.comops.api.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import yowyob.comops.api.auth.application.service.AuthEmailVerificationTokenService.IssuedEmailVerificationToken;
import yowyob.comops.api.auth.application.service.AuthEmailVerificationTokenService.VerifiedEmailVerificationToken;
import yowyob.comops.api.auth.domain.model.UserAccount;
import yowyob.comops.api.kernel.config.JwtTokenService;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Tests unitaires pour AuthEmailVerificationTokenService.
 *
 * Ce service gère les tokens du flux "Vérification d'email" :
 *
 * Séquence :
 *   1. Après inscription, un token (valable 24h) est émis via issue(userAccount)
 *   2. L'utilisateur clique sur le lien → verify(token)
 *   3. Si valide → confirmEmailVerification() marque emailVerified=true
 *
 * Analogie : c'est comme le lien "confirmez votre adresse email" que tu reçois
 * après t'être inscrit sur un site. Il expire après 24 heures.
 */
class AuthEmailVerificationTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private AuthEmailVerificationTokenService emailVerificationService;

    @BeforeEach
    void setUp() {
        SecurityRuntimeProperties props = new SecurityRuntimeProperties();
        props.getJwt().setAutoGenerateKeyPair(true);
        props.getJwt().setIssuer("yowyob-test");
        jwtTokenService = new JwtTokenService(props);
        emailVerificationService = new AuthEmailVerificationTokenService(jwtTokenService);
    }

    /** Crée un UserAccount en mémoire pour les tests. */
    private UserAccount makeAccount(UUID tenantId, String username, String email) {
        return UserAccount.register(
                tenantId, UUID.randomUUID(), username, email,
                new BCryptPasswordEncoder().encode("Passw0rd!secure"), "LOCAL");
    }

    // =========================================================================
    // issue() — émission du token de vérification
    // =========================================================================

    @Nested
    @DisplayName("issue() — émission du token de vérification d'email")
    class Issue {

        @Test
        @DisplayName("émet un token non vide avec une durée de vie de 24 heures")
        void shouldIssueTokenWithCorrectTtl() {
            UserAccount account = makeAccount(UUID.randomUUID(), "jean", "jean@example.com");

            IssuedEmailVerificationToken issued = emailVerificationService.issue(account);

            assertThat(issued.token()).isNotBlank();
            assertThat(issued.expiresInSeconds())
                    .as("TTL de vérification email : 24h = 86400 secondes")
                    .isEqualTo(86_400L);
        }

        @Test
        @DisplayName("deux comptes différents produisent deux tokens différents")
        void shouldIssueDistinctTokensForDifferentAccounts() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account1 = makeAccount(tenantId, "jean", "jean@example.com");
            UserAccount account2 = makeAccount(tenantId, "marie", "marie@example.com");

            IssuedEmailVerificationToken token1 = emailVerificationService.issue(account1);
            IssuedEmailVerificationToken token2 = emailVerificationService.issue(account2);

            assertThat(token1.token()).isNotEqualTo(token2.token());
        }
    }

    // =========================================================================
    // verify() — vérification du token
    // =========================================================================

    @Nested
    @DisplayName("verify() — vérification et décodage du token")
    class Verify {

        @Test
        @DisplayName("un token valide est vérifié et les données sont extraites")
        void shouldVerifyValidTokenAndExtractData() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = makeAccount(tenantId, "jean", "jean@example.com");

            IssuedEmailVerificationToken issued = emailVerificationService.issue(account);

            Optional<VerifiedEmailVerificationToken> verified =
                    emailVerificationService.verify(issued.token());

            assertThat(verified).isPresent();
            assertThat(verified.get().tenantId()).isEqualTo(tenantId);
            assertThat(verified.get().userId()).isEqualTo(account.id());
            assertThat(verified.get().actorId()).isEqualTo(account.actorId());
            assertThat(verified.get().email()).isEqualTo("jean@example.com");
            assertThat(verified.get().subjectUserId()).isEqualTo(account.id());
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token corrompu")
        void shouldRejectTamperedToken() {
            UserAccount account = makeAccount(UUID.randomUUID(), "jean", "jean@example.com");
            IssuedEmailVerificationToken issued = emailVerificationService.issue(account);

            String tampered = issued.token().substring(0, issued.token().length() - 8) + "CORROMPU";

            assertThat(emailVerificationService.verify(tampered)).isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token null ou vide")
        void shouldRejectNullOrBlankToken() {
            assertThat(emailVerificationService.verify(null)).isEmpty();
            assertThat(emailVerificationService.verify("")).isEmpty();
            assertThat(emailVerificationService.verify("   ")).isEmpty();
        }

        @Test
        @DisplayName("rejette un token d'un autre service (mauvais type)")
        void shouldRejectTokenFromAnotherService() {
            // Un token SSO ne doit pas être accepté comme token de vérification d'email
            SecurityRuntimeProperties props = new SecurityRuntimeProperties();
            props.getJwt().setAutoGenerateKeyPair(true);
            props.getJwt().setIssuer("yowyob-test");
            JwtTokenService sameJwt = new JwtTokenService(props);

            AuthPasswordResetTokenService otherService =
                    new AuthPasswordResetTokenService(sameJwt);

            // on émet un token de reset avec le même JwtTokenService
            UserAccount account = makeAccount(UUID.randomUUID(), "jean", "jean@example.com");
            String resetToken = otherService.issueReset(account).token();

            assertThat(emailVerificationService.verify(resetToken))
                    .as("Un token de reset ne doit pas être accepté comme token de vérification email")
                    .isEmpty();
        }

        @Test
        @DisplayName("le subject du token est l'userId du compte")
        void subjectShouldBeUserId() {
            UserAccount account = makeAccount(UUID.randomUUID(), "jean", "jean@example.com");
            IssuedEmailVerificationToken issued = emailVerificationService.issue(account);

            VerifiedEmailVerificationToken verified =
                    emailVerificationService.verify(issued.token()).orElseThrow();

            assertThat(verified.subjectUserId()).isEqualTo(account.id());
        }
    }
}
