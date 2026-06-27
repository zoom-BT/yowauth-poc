package yowyob.comops.api.kernel.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.application.port.out.OrganizationServiceRuntimeEntitlementDirectory;
import yowyob.comops.api.kernel.config.ApiKeyAuthenticationToken;
import yowyob.comops.api.kernel.config.OrganizationServiceRequestQuotaProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class OrganizationServiceEntitlementWebFilter implements WebFilter {

    private final OrganizationServiceRuntimeEntitlementDirectory organizationServiceRuntimeEntitlementDirectory;
    private final PlatformServiceRouteResolver routeResolver;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final OrganizationServiceRequestQuotaProperties quotaProperties;
    private final ObjectMapper objectMapper;
    private final Counter allowedCounter;
    private final Counter rejectedCounter;
    private final Counter failOpenCounter;

    public OrganizationServiceEntitlementWebFilter(
            OrganizationServiceRuntimeEntitlementDirectory organizationServiceRuntimeEntitlementDirectory,
            PlatformServiceRouteResolver routeResolver,
            ReactiveStringRedisTemplate redisTemplate,
            OrganizationServiceRequestQuotaProperties quotaProperties,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.organizationServiceRuntimeEntitlementDirectory = organizationServiceRuntimeEntitlementDirectory;
        this.routeResolver = routeResolver;
        this.redisTemplate = redisTemplate;
        this.quotaProperties = quotaProperties;
        this.objectMapper = objectMapper;
        this.allowedCounter = Counter.builder("iwm.quotas.organization_service_requests.allowed")
                .description("Number of organization service requests accepted by the organization quota filter")
                .register(meterRegistry);
        this.rejectedCounter = Counter.builder("iwm.quotas.organization_service_requests.rejected")
                .description("Number of organization service requests rejected by the organization quota filter")
                .register(meterRegistry);
        this.failOpenCounter = Counter.builder("iwm.quotas.organization_service_requests.fail_open")
                .description("Number of organization service quota failures bypassed in fail-open mode")
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String serviceCode = routeResolver.resolveOrganizationEntitlementServiceCode(
                exchange.getRequest().getPath().pathWithinApplication().value());
        if (serviceCode == null) {
            return chain.filter(exchange);
        }
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(securityContext -> securityContext.getAuthentication())
                .mapNotNull(authentication -> authentication instanceof ApiKeyAuthenticationToken token ? token : null)
                .flatMap(token -> enforce(exchange, chain, token, serviceCode).thenReturn(Boolean.TRUE))
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange).thenReturn(Boolean.TRUE)))
                .then();
    }

    private Mono<Void> enforce(ServerWebExchange exchange, WebFilterChain chain, ApiKeyAuthenticationToken token,
            String serviceCode) {
        if (token.tenantId() == null) {
            return writeFailure(exchange, HttpStatus.BAD_REQUEST, "X-Tenant-Id is required for service-scoped endpoints.",
                    "TENANT_CONTEXT_REQUIRED");
        }
        UUID organizationId = token.organizationId() != null
                ? token.organizationId()
                : parseOrganizationId(exchange);
        if (organizationId == null) {
            return writeFailure(exchange, HttpStatus.BAD_REQUEST,
                    "X-Organization-Id is required for organization-scoped business service endpoints.",
                    "ORGANIZATION_CONTEXT_REQUIRED");
        }
        return organizationServiceRuntimeEntitlementDirectory
                .resolveRuntimeEntitlement(token.tenantId(), organizationId, serviceCode)
                .flatMap(entitlement -> entitlement.effective()
                        ? enforceQuota(exchange, chain, token, organizationId, entitlement)
                        : writeFailure(exchange, HttpStatus.FORBIDDEN,
                                "Organization is not subscribed to service " + serviceCode + ".",
                                "ORGANIZATION_SERVICE_NOT_SUBSCRIBED"));
    }

    private Mono<Void> enforceQuota(ServerWebExchange exchange, WebFilterChain chain, ApiKeyAuthenticationToken token,
            UUID organizationId,
            yowyob.comops.api.kernel.application.port.out.OrganizationServiceRuntimeEntitlement entitlement) {
        if (!quotaProperties.isEnabled()
                || redisTemplate == null
                || entitlement.requestQuotaLimit() == null
                || entitlement.requestQuotaWindowSeconds() == null) {
            return chain.filter(exchange);
        }
        long windowSeconds = Math.max(1L, entitlement.requestQuotaWindowSeconds());
        long bucket = Instant.now().getEpochSecond() / windowSeconds;
        String key = quotaProperties.getKeyPrefix()
                + ":" + token.tenantId()
                + ":" + organizationId
                + ":" + entitlement.serviceCode()
                + ":" + bucket;
        Duration ttl = Duration.ofSeconds(windowSeconds).plusSeconds(5);

        return redisTemplate.opsForValue().increment(key)
                .flatMap(currentCount -> {
                    Mono<Boolean> ttlMono = currentCount != null && currentCount == 1L
                            ? redisTemplate.expire(key, ttl)
                            : Mono.just(Boolean.FALSE);
                    return ttlMono.then(Mono.defer(() -> {
                        long count = currentCount == null ? 0L : currentCount;
                        long remaining = Math.max(0L, entitlement.requestQuotaLimit() - count);
                        exchange.getResponse().getHeaders().set("X-IWM-Organization-Quota-Limit",
                                Long.toString(entitlement.requestQuotaLimit()));
                        exchange.getResponse().getHeaders().set("X-IWM-Organization-Quota-Remaining",
                                Long.toString(remaining));
                        exchange.getResponse().getHeaders().set("X-IWM-Organization-Quota-Window-Seconds",
                                Long.toString(windowSeconds));
                        exchange.getResponse().getHeaders().set("X-IWM-Organization-Quota-Scope",
                                "organization-service");
                        exchange.getResponse().getHeaders().set("X-IWM-Organization-Quota-Organization-Id",
                                organizationId.toString());
                        exchange.getResponse().getHeaders().set("X-IWM-Organization-Quota-Service",
                                entitlement.serviceCode());
                        if (count > entitlement.requestQuotaLimit()) {
                            rejectedCounter.increment();
                            exchange.getResponse().getHeaders().set("Retry-After", Long.toString(windowSeconds));
                            return writeFailure(exchange, HttpStatus.TOO_MANY_REQUESTS,
                                    "Organization service request quota exceeded.",
                                    "ORGANIZATION_SERVICE_QUOTA_EXCEEDED");
                        }
                        allowedCounter.increment();
                        return chain.filter(exchange);
                    }));
                })
                .onErrorResume(error -> {
                    if (quotaProperties.isFailOpen()) {
                        failOpenCounter.increment();
                        return chain.filter(exchange);
                    }
                    return writeFailure(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                            "Organization service request quota service unavailable.",
                            "ORGANIZATION_SERVICE_QUOTA_UNAVAILABLE");
                });
    }

    private UUID parseOrganizationId(ServerWebExchange exchange) {
        String raw = exchange.getRequest().getQueryParams().getFirst("organizationId");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Mono<Void> writeFailure(ServerWebExchange exchange, HttpStatus status, String message, String errorCode) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(ApiResponse.failure(message, errorCode));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            payload = ("{\"success\":false,\"message\":\"" + message.replace("\"", "'")
                    + "\",\"errorCode\":\"" + errorCode + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }
}
