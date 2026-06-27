package yowyob.comops.api.auth.application.service;

import com.nimbusds.jwt.JWTClaimsSet;
import java.text.ParseException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import yowyob.comops.api.auth.domain.model.UserAccount;
import yowyob.comops.api.kernel.config.JwtTokenService;

@Component
public class AuthEmailVerificationTokenService {

    private static final String TOKEN_TYPE = "auth-email-verification";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final JwtTokenService jwtTokenService;

    public AuthEmailVerificationTokenService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public IssuedEmailVerificationToken issue(UserAccount userAccount) {
        String token = jwtTokenService.issueSignedToken(
                userAccount.id().toString(),
                DEFAULT_TTL,
                Map.of(
                        "typ", TOKEN_TYPE,
                        "tenantId", userAccount.tenantId().toString(),
                        "userId", userAccount.id().toString(),
                        "actorId", userAccount.actorId().toString(),
                        "email", userAccount.email()));
        return new IssuedEmailVerificationToken(token, DEFAULT_TTL.getSeconds());
    }

    public Optional<VerifiedEmailVerificationToken> verify(String token) {
        return jwtTokenService.decodeSignedToken(token)
                .filter(claims -> hasExpectedType(claims))
                .map(claims -> new VerifiedEmailVerificationToken(
                        UUID.fromString(claims.getSubject()),
                        UUID.fromString(String.valueOf(claims.getClaim("tenantId"))),
                        UUID.fromString(String.valueOf(claims.getClaim("userId"))),
                        UUID.fromString(String.valueOf(claims.getClaim("actorId"))),
                        String.valueOf(claims.getClaim("email"))));
    }

    private boolean hasExpectedType(JWTClaimsSet claims) {
        try {
            return TOKEN_TYPE.equals(claims.getStringClaim("typ"));
        } catch (ParseException exception) {
            return false;
        }
    }

    public record IssuedEmailVerificationToken(String token, long expiresInSeconds) {
    }

    public record VerifiedEmailVerificationToken(
            UUID subjectUserId,
            UUID tenantId,
            UUID userId,
            UUID actorId,
            String email) {
    }
}
