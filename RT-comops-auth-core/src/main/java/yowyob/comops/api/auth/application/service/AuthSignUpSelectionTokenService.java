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
import yowyob.comops.api.auth.application.port.in.SelectableSignUpContext;
import yowyob.comops.api.kernel.config.JwtTokenService;

@Component
public class AuthSignUpSelectionTokenService {

    private static final String TOKEN_TYPE = "auth-sign-up-selection";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final JwtTokenService jwtTokenService;

    public AuthSignUpSelectionTokenService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public IssuedSignUpSelectionToken issue(String organizationCode, List<SelectableSignUpContext> contexts) {
        Objects.requireNonNull(organizationCode, "organizationCode is required");
        Objects.requireNonNull(contexts, "contexts are required");
        if (contexts.isEmpty()) {
            throw new IllegalArgumentException("At least one sign-up context is required.");
        }
        String token = jwtTokenService.issueSignedToken(
                organizationCode.trim().toUpperCase(),
                DEFAULT_TTL,
                Map.of(
                        "typ", TOKEN_TYPE,
                        "organizationCode", organizationCode.trim().toUpperCase(),
                        "contexts", contexts.stream().map(this::toClaim).toList()));
        return new IssuedSignUpSelectionToken(token, DEFAULT_TTL.getSeconds());
    }

    public Optional<VerifiedSignUpSelectionToken> verify(String token) {
        return jwtTokenService.decodeSignedToken(token)
                .filter(this::hasExpectedType)
                .map(this::toVerifiedToken);
    }

    @SuppressWarnings("unchecked")
    private VerifiedSignUpSelectionToken toVerifiedToken(JWTClaimsSet claims) {
        List<Map<String, Object>> rawContexts = (List<Map<String, Object>>) claims.getClaim("contexts");
        List<VerifiedSelectableSignUpContext> contexts = rawContexts == null
                ? List.of()
                : rawContexts.stream()
                        .map(entry -> new VerifiedSelectableSignUpContext(
                                String.valueOf(entry.get("contextId")),
                                UUID.fromString(String.valueOf(entry.get("tenantId"))),
                                UUID.fromString(String.valueOf(entry.get("organizationId"))),
                                String.valueOf(entry.get("organizationCode")),
                                String.valueOf(entry.get("organizationName")),
                                String.valueOf(entry.get("organizationType"))))
                        .toList();
        return new VerifiedSignUpSelectionToken(claims.getSubject(), contexts);
    }

    private Map<String, Object> toClaim(SelectableSignUpContext context) {
        Map<String, Object> claim = new java.util.LinkedHashMap<>();
        claim.put("contextId", context.contextId());
        claim.put("tenantId", context.tenantId().toString());
        claim.put("organizationId", context.organizationId().toString());
        claim.put("organizationCode", context.organizationCode());
        claim.put("organizationName", context.organizationName());
        claim.put("organizationType", context.organizationType());
        return claim;
    }

    private boolean hasExpectedType(JWTClaimsSet claims) {
        try {
            return TOKEN_TYPE.equals(claims.getStringClaim("typ"));
        } catch (ParseException exception) {
            return false;
        }
    }

    public record IssuedSignUpSelectionToken(String token, long expiresInSeconds) {
    }

    public record VerifiedSignUpSelectionToken(
            String organizationCode,
            List<VerifiedSelectableSignUpContext> contexts) {
    }

    public record VerifiedSelectableSignUpContext(
            String contextId,
            UUID tenantId,
            UUID organizationId,
            String organizationCode,
            String organizationName,
            String organizationType) {
    }
}
