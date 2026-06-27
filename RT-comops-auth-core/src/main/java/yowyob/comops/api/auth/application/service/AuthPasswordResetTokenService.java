package yowyob.comops.api.auth.application.service;

import com.nimbusds.jwt.JWTClaimsSet;
import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import yowyob.comops.api.auth.domain.model.UserAccount;
import yowyob.comops.api.kernel.config.JwtTokenService;

@Component
public class AuthPasswordResetTokenService {

    private static final String SELECTION_TYPE = "auth-password-reset-selection";
    private static final String RESET_TYPE = "auth-password-reset";
    private static final Duration SELECTION_TTL = Duration.ofMinutes(10);
    private static final Duration RESET_TTL = Duration.ofMinutes(15);

    private final JwtTokenService jwtTokenService;

    public AuthPasswordResetTokenService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public IssuedPasswordResetSelectionToken issueSelection(String principal, List<PasswordResetContext> contexts) {
        Objects.requireNonNull(principal, "principal is required");
        Objects.requireNonNull(contexts, "contexts are required");
        if (contexts.isEmpty()) {
            throw new IllegalArgumentException("At least one password reset context is required.");
        }
        String token = jwtTokenService.issueSignedToken(
                principal.trim().toLowerCase(),
                SELECTION_TTL,
                Map.of(
                        "typ", SELECTION_TYPE,
                        "principal", principal.trim().toLowerCase(),
                        "contexts", contexts.stream().map(this::toClaim).toList()));
        return new IssuedPasswordResetSelectionToken(token, SELECTION_TTL.getSeconds());
    }

    public Optional<VerifiedPasswordResetSelectionToken> verifySelection(String token) {
        return jwtTokenService.decodeSignedToken(token)
                .filter(claims -> hasType(claims, SELECTION_TYPE))
                .map(this::toVerifiedSelection);
    }

    public IssuedPasswordResetToken issueReset(UserAccount userAccount) {
        String token = jwtTokenService.issueSignedToken(
                userAccount.id().toString(),
                RESET_TTL,
                Map.of(
                        "typ", RESET_TYPE,
                        "tenantId", userAccount.tenantId().toString(),
                        "userId", userAccount.id().toString(),
                        "actorId", userAccount.actorId().toString(),
                        "email", userAccount.email()));
        return new IssuedPasswordResetToken(token, RESET_TTL.getSeconds());
    }

    public Optional<VerifiedPasswordResetToken> verifyReset(String token) {
        return jwtTokenService.decodeSignedToken(token)
                .filter(claims -> hasType(claims, RESET_TYPE))
                .map(this::toVerifiedReset);
    }

    @SuppressWarnings("unchecked")
    private VerifiedPasswordResetSelectionToken toVerifiedSelection(JWTClaimsSet claims) {
        List<Map<String, Object>> rawContexts = (List<Map<String, Object>>) claims.getClaim("contexts");
        List<VerifiedPasswordResetContext> contexts = rawContexts == null
                ? List.of()
                : rawContexts.stream()
                        .map(entry -> new VerifiedPasswordResetContext(
                                String.valueOf(entry.get("contextId")),
                                UUID.fromString(String.valueOf(entry.get("tenantId"))),
                                UUID.fromString(String.valueOf(entry.get("userId"))),
                                UUID.fromString(String.valueOf(entry.get("actorId"))),
                                String.valueOf(entry.get("username")),
                                String.valueOf(entry.get("email"))))
                        .toList();
        return new VerifiedPasswordResetSelectionToken(claims.getSubject(), contexts);
    }

    private VerifiedPasswordResetToken toVerifiedReset(JWTClaimsSet claims) {
        return new VerifiedPasswordResetToken(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(String.valueOf(claims.getClaim("tenantId"))),
                UUID.fromString(String.valueOf(claims.getClaim("userId"))),
                UUID.fromString(String.valueOf(claims.getClaim("actorId"))),
                String.valueOf(claims.getClaim("email")));
    }

    private Map<String, Object> toClaim(PasswordResetContext context) {
        Map<String, Object> claim = new java.util.LinkedHashMap<>();
        claim.put("contextId", context.contextId());
        claim.put("tenantId", context.tenantId().toString());
        claim.put("userId", context.userId().toString());
        claim.put("actorId", context.actorId().toString());
        claim.put("username", context.username());
        claim.put("email", context.email());
        return claim;
    }

    private boolean hasType(JWTClaimsSet claims, String expectedType) {
        try {
            return expectedType.equals(claims.getStringClaim("typ"));
        } catch (ParseException exception) {
            return false;
        }
    }

    public record PasswordResetContext(
            String contextId,
            UUID tenantId,
            UUID userId,
            UUID actorId,
            String username,
            String email) {
    }

    public record IssuedPasswordResetSelectionToken(String token, long expiresInSeconds) {
    }

    public record IssuedPasswordResetToken(String token, long expiresInSeconds) {
    }

    public record VerifiedPasswordResetSelectionToken(
            String principal,
            List<VerifiedPasswordResetContext> contexts) {
    }

    public record VerifiedPasswordResetContext(
            String contextId,
            UUID tenantId,
            UUID userId,
            UUID actorId,
            String username,
            String email) {
    }

    public record VerifiedPasswordResetToken(
            UUID subjectUserId,
            UUID tenantId,
            UUID userId,
            UUID actorId,
            String email) {
    }
}
