package yowyob.comops.api.kernel.config;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public final class ApiKeyServerAuthenticationConverter implements ServerAuthenticationConverter {

    public static final String API_KEY_HEADER = "X-Api-Key";
    public static final String CLIENT_ID_HEADER = "X-Client-Id";
    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String ORGANIZATION_HEADER = "X-Organization-Id";
    public static final String AGENCY_HEADER = "X-Agency-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    private final UserSessionTokenService userSessionTokenService;

    public ApiKeyServerAuthenticationConverter(UserSessionTokenService userSessionTokenService) {
        this.userSessionTokenService = userSessionTokenService;
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        UserSessionTokenClaims claims = resolveSessionClaims(exchange);
        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        String clientId = exchange.getRequest().getHeaders().getFirst(CLIENT_ID_HEADER);
        if (apiKey == null || apiKey.isBlank() || clientId == null || clientId.isBlank()) {
            return Mono.empty();
        }
        return Mono.just(ApiKeyAuthenticationToken.unauthenticated(clientId.trim(), apiKey,
                firstNonNull(parseUuid(exchange, TENANT_HEADER), claims == null ? null : claims.tenantId()),
                firstNonNull(parseUuid(exchange, ORGANIZATION_HEADER), claims == null ? null : claims.organizationId()),
                firstNonNull(parseUuid(exchange, AGENCY_HEADER), claims == null ? null : claims.agencyId()),
                claims == null ? null : claims.userId(),
                claims == null ? null : claims.actorId())).cast(Authentication.class);
    }

    private UserSessionTokenClaims resolveSessionClaims(ServerWebExchange exchange) {
        return userSessionTokenService.verify(extractBearerToken(exchange)).orElse(null);
    }

    private UUID parseUuid(ServerWebExchange exchange, String headerName) {
        String rawValue = exchange.getRequest().getHeaders().getFirst(headerName);
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String extractBearerToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    private UUID firstNonNull(UUID preferred, UUID fallback) {
        return preferred != null ? preferred : fallback;
    }
}
