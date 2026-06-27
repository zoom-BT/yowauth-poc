package yowyob.comops.api.auth.application.service;

import com.nimbusds.jwt.JWTClaimsSet;
import yowyob.comops.api.auth.application.port.in.SelectableLoginContext;
import yowyob.comops.api.kernel.config.JwtTokenService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AuthContextSelectionTokenService {

    private static final String TOKEN_TYPE = "auth-context-selection";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final JwtTokenService jwtTokenService;

    public AuthContextSelectionTokenService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public IssuedContextSelectionToken issue(String principal, List<SelectableLoginContext> contexts) {
        Objects.requireNonNull(principal, "principal is required");
        Objects.requireNonNull(contexts, "contexts are required");
        if (contexts.isEmpty()) {
            throw new IllegalArgumentException("At least one login context is required.");
        }
        String token = jwtTokenService.issueSignedToken(
                principal.trim(),
                DEFAULT_TTL,
                Map.of(
                        "typ", TOKEN_TYPE,
                        "principal", principal.trim(),
                        "contexts", contexts.stream()
                                .map(this::toContextClaim)
                                .toList()));
        return new IssuedContextSelectionToken(token, DEFAULT_TTL.getSeconds());
    }

    public Optional<VerifiedContextSelectionToken> verify(String token) {
        return jwtTokenService.decodeSignedToken(token)
                .filter(claims -> hasExpectedType(claims))
                .map(this::toVerifiedToken);
    }

    @SuppressWarnings("unchecked")
    private VerifiedContextSelectionToken toVerifiedToken(JWTClaimsSet claims) {
        List<Map<String, Object>> rawContexts = (List<Map<String, Object>>) claims.getClaim("contexts");
        List<VerifiedSelectableLoginContext> contexts = rawContexts == null
                ? List.of()
                : rawContexts.stream()
                        .map(entry -> new VerifiedSelectableLoginContext(
                                String.valueOf(entry.get("contextId")),
                                java.util.UUID.fromString(String.valueOf(entry.get("tenantId"))),
                                java.util.UUID.fromString(String.valueOf(entry.get("userId"))),
                                entry.get("actorId") == null ? null
                                        : java.util.UUID.fromString(String.valueOf(entry.get("actorId")))))
                        .toList();
        return new VerifiedContextSelectionToken(claims.getSubject(), contexts);
    }

    private Map<String, Object> toContextClaim(SelectableLoginContext context) {
        Map<String, Object> claim = new java.util.LinkedHashMap<>();
        claim.put("contextId", context.contextId());
        claim.put("tenantId", context.tenantId().toString());
        claim.put("userId", context.userId().toString());
        if (context.actorId() != null) {
            claim.put("actorId", context.actorId().toString());
        }
        return claim;
    }

    private boolean hasExpectedType(JWTClaimsSet claims) {
        try {
            return TOKEN_TYPE.equals(claims.getStringClaim("typ"));
        } catch (java.text.ParseException exception) {
            return false;
        }
    }

    public record IssuedContextSelectionToken(String token, long expiresInSeconds) {
    }

    public record VerifiedContextSelectionToken(
            String principal,
            List<VerifiedSelectableLoginContext> contexts) {
    }

    public record VerifiedSelectableLoginContext(
            String contextId,
            java.util.UUID tenantId,
            java.util.UUID userId,
            java.util.UUID actorId) {
    }
}
