package yowyob.comops.api.auth.application.service;

import com.nimbusds.jwt.JWTClaimsSet;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import yowyob.comops.api.kernel.config.JwtTokenService;

@Component
public class AuthChallengeTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String OTP_TYPE = "auth-otp-challenge";
    private static final String CAPTCHA_TYPE = "auth-captcha-challenge";
    private static final String CAPTCHA_VERIFICATION_TYPE = "auth-captcha-verification";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);
    private static final Duration CAPTCHA_VERIFICATION_TTL = Duration.ofMinutes(10);

    private final JwtTokenService jwtTokenService;

    public AuthChallengeTokenService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public IssuedOtpChallenge issueOtp(String purpose, String channel, String recipient, UUID tenantId, UUID userId) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String token = jwtTokenService.issueSignedToken(
                userId == null ? recipient : userId.toString(),
                OTP_TTL,
                Map.of(
                        "typ", OTP_TYPE,
                        "purpose", normalize(purpose),
                        "channel", normalize(channel),
                        "recipient", recipient,
                        "tenantId", tenantId == null ? "" : tenantId.toString(),
                        "userId", userId == null ? "" : userId.toString(),
                        // On ne stocke JAMAIS le code en clair dans le jeton (payload JWT lisible) :
                        // seulement son HMAC à clé serveur. Le code réel ne circule que par e-mail/SMS.
                        "codeHash", jwtTokenService.deriveHmacSha256(code)));
        return new IssuedOtpChallenge(token, code, OTP_TTL.getSeconds());
    }

    public Optional<VerifiedOtpChallenge> verifyOtp(String token, String code, String expectedPurpose) {
        String normalizedCode = code == null ? "" : code.trim();
        return jwtTokenService.decodeSignedToken(token)
                .filter(claims -> hasType(claims, OTP_TYPE))
                .filter(claims -> normalize(expectedPurpose).equals(readString(claims, "purpose")))
                .filter(claims -> constantTimeEquals(
                        readString(claims, "codeHash"), jwtTokenService.deriveHmacSha256(normalizedCode)))
                .map(claims -> new VerifiedOtpChallenge(
                        readString(claims, "purpose"),
                        readString(claims, "channel"),
                        readString(claims, "recipient"),
                        parseUuid(readString(claims, "tenantId")),
                        parseUuid(readString(claims, "userId"))));
    }

    public IssuedCaptchaChallenge issueCaptcha() {
        int left = 10 + RANDOM.nextInt(40);
        int right = 10 + RANDOM.nextInt(40);
        String answer = String.valueOf(left + right);
        String token = jwtTokenService.issueSignedToken(
                UUID.randomUUID().toString(),
                CAPTCHA_TTL,
                Map.of(
                        "typ", CAPTCHA_TYPE,
                        "answerHash", jwtTokenService.deriveHmacSha256(answer)));
        return new IssuedCaptchaChallenge(token, left + " + " + right, answer, CAPTCHA_TTL.getSeconds());
    }

    public Optional<IssuedCaptchaVerification> verifyCaptcha(String challengeToken, String answer) {
        String normalizedAnswer = answer == null ? "" : answer.trim();
        return jwtTokenService.decodeSignedToken(challengeToken)
                .filter(claims -> hasType(claims, CAPTCHA_TYPE))
                .filter(claims -> constantTimeEquals(
                        readString(claims, "answerHash"), jwtTokenService.deriveHmacSha256(normalizedAnswer)))
                .map(claims -> {
                    String token = jwtTokenService.issueSignedToken(
                            claims.getSubject(),
                            CAPTCHA_VERIFICATION_TTL,
                            Map.of("typ", CAPTCHA_VERIFICATION_TYPE));
                    return new IssuedCaptchaVerification(token, CAPTCHA_VERIFICATION_TTL.getSeconds());
                });
    }

    public boolean verifyCaptchaVerification(String token) {
        return token != null && jwtTokenService.decodeSignedToken(token)
                .filter(claims -> hasType(claims, CAPTCHA_VERIFICATION_TYPE))
                .isPresent();
    }

    private boolean hasType(JWTClaimsSet claims, String type) {
        try {
            return type.equals(claims.getStringClaim("typ"));
        } catch (ParseException exception) {
            return false;
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                actual.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String readString(JWTClaimsSet claims, String claim) {
        Object value = claims.getClaim(claim);
        return value == null ? null : String.valueOf(value);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_');
    }

    public record IssuedOtpChallenge(String token, String codePreview, long expiresInSeconds) {
    }

    public record VerifiedOtpChallenge(String purpose, String channel, String recipient, UUID tenantId, UUID userId) {
    }

    public record IssuedCaptchaChallenge(String token, String prompt, String answerPreview, long expiresInSeconds) {
    }

    public record IssuedCaptchaVerification(String token, long expiresInSeconds) {
    }
}
