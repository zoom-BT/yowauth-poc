package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.kernel.application.service.ReactiveRequestContextHolder;
import yowyob.comops.api.kernel.config.UserSessionTokenClaims;
import yowyob.comops.api.kernel.config.UserSessionTokenService;
import yowyob.comops.api.kernel.domain.model.TenantContext;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TenantWebFilter implements WebFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String ORGANIZATION_HEADER = "X-Organization-Id";
    public static final String AGENCY_HEADER = "X-Agency-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    private final UserSessionTokenService userSessionTokenService;

    public TenantWebFilter(UserSessionTokenService userSessionTokenService) {
        this.userSessionTokenService = userSessionTokenService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        UserSessionTokenClaims claims = userSessionTokenService.verify(extractBearerToken(request))
                .orElse(null);
        UUID tenantId = parseUuidHeader(request, TENANT_HEADER);
        if (tenantId == null && claims != null) {
            tenantId = claims.tenantId();
        }
        if (tenantId == null) {
            return chain.filter(exchange);
        }

        TenantContext tenantContext = new TenantContext(
                tenantId,
                firstNonNull(parseUuidHeader(request, ORGANIZATION_HEADER), claims == null ? null : claims.organizationId()),
                firstNonNull(parseUuidHeader(request, AGENCY_HEADER), claims == null ? null : claims.agencyId()),
                claims == null ? null : claims.userId(),
                claims == null ? null : claims.actorId());

        return chain.filter(exchange)
                .contextWrite(context -> ReactiveRequestContextHolder.withTenantContext(context, tenantContext))
                .doOnSubscribe(s -> applyTenantMdc(tenantContext))
                .doFinally(signal -> clearTenantMdc());
    }

    private void applyTenantMdc(TenantContext ctx) {
        if (ctx.tenantId() != null) {
            MDC.put("tenantId", ctx.tenantId().toString());
        }
        if (ctx.organizationId() != null) {
            MDC.put("organizationId", ctx.organizationId().toString());
        }
        if (ctx.userId() != null) {
            MDC.put("userId", ctx.userId().toString());
        }
        if (ctx.actorId() != null) {
            MDC.put("actorId", ctx.actorId().toString());
        }
    }

    private void clearTenantMdc() {
        MDC.remove("tenantId");
        MDC.remove("organizationId");
        MDC.remove("userId");
        MDC.remove("actorId");
    }

    private UUID parseUuidHeader(ServerHttpRequest request, String headerName) {
        List<String> values = request.getHeaders().get(headerName);
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(values.getFirst());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String extractBearerToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
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
