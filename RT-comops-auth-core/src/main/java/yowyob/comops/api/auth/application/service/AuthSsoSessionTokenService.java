package yowyob.comops.api.auth.application.service;

import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import yowyob.comops.api.auth.config.AuthSsoProperties;
import yowyob.comops.api.kernel.config.JwtTokenService;

@Component
public class AuthSsoSessionTokenService {

    private static final String TOKEN_TYPE = "auth-sso-session";

    private final JwtTokenService jwtTokenService;
    private final Duration sessionTtl;

    public AuthSsoSessionTokenService(JwtTokenService jwtTokenService, AuthSsoProperties authSsoProperties) {
        this.jwtTokenService = jwtTokenService;
        this.sessionTtl = authSsoProperties.getSessionTtl() == null
                ? Duration.ofHours(8)
                : authSsoProperties.getSessionTtl();
    }

    public IssuedSsoSessionToken issue(String subject, List<SsoSessionContext> contexts) {
        Objects.requireNonNull(subject, "subject is required");
        Objects.requireNonNull(contexts, "contexts are required");
        if (contexts.isEmpty()) {
            throw new IllegalArgumentException("At least one SSO context is required.");
        }
        String token = jwtTokenService.issueSignedToken(
                subject.trim(),
                sessionTtl,
                Map.of(
                        "typ", TOKEN_TYPE,
                        "principal", subject.trim(),
                        "contexts", contexts.stream().map(this::toContextClaim).toList()));
        return new IssuedSsoSessionToken(token, sessionTtl.getSeconds());
    }

    public Optional<VerifiedSsoSessionToken> verify(String token) {
        return jwtTokenService.decodeSignedToken(token)
                .filter(this::hasExpectedType)
                .map(this::toVerifiedToken);
    }

    @SuppressWarnings("unchecked")
    private VerifiedSsoSessionToken toVerifiedToken(JWTClaimsSet claims) {
        List<Map<String, Object>> rawContexts = (List<Map<String, Object>>) claims.getClaim("contexts");
        List<VerifiedSsoSessionContext> contexts = rawContexts == null
                ? List.of()
                : rawContexts.stream()
                        .map(entry -> new VerifiedSsoSessionContext(
                                String.valueOf(entry.get("contextId")),
                                UUID.fromString(String.valueOf(entry.get("tenantId"))),
                                UUID.fromString(String.valueOf(entry.get("userId"))),
                                entry.get("actorId") == null ? null
                                        : UUID.fromString(String.valueOf(entry.get("actorId")))))
                        .toList();
        return new VerifiedSsoSessionToken(claims.getSubject(), claims.getJWTID(), contexts);
    }

    private Map<String, Object> toContextClaim(SsoSessionContext context) {
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

    public record SsoSessionContext(
            String contextId,
            UUID tenantId,
            UUID userId,
            UUID actorId) {
    }

    public record IssuedSsoSessionToken(
            String token,
            long expiresInSeconds) {
    }

    public record VerifiedSsoSessionToken(
            String subject,
            String sessionId,
            List<VerifiedSsoSessionContext> contexts) {
    }

    public record VerifiedSsoSessionContext(
            String contextId,
            UUID tenantId,
            UUID userId,
            UUID actorId) {
    }
}
