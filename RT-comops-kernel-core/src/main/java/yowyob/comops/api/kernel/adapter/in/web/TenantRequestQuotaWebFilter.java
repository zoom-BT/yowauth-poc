package yowyob.comops.api.kernel.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.config.ApiKeyAuthenticationToken;
import yowyob.comops.api.kernel.config.TenantRequestQuotaProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-50)
@ConditionalOnProperty(prefix = "iwm.quotas.tenant-requests", name = "enabled", havingValue = "true")
public class TenantRequestQuotaWebFilter implements WebFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String CLIENT_ID_HEADER = "X-Client-Id";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final TenantRequestQuotaProperties properties;
    private final ObjectMapper objectMapper;
    private final PlatformServiceRouteResolver routeResolver;
    private final Counter allowedCounter;
    private final Counter rejectedCounter;
    private final Counter failOpenCounter;

    public TenantRequestQuotaWebFilter(ReactiveStringRedisTemplate redisTemplate,
            TenantRequestQuotaProperties properties,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.routeResolver = new PlatformServiceRouteResolver();
        this.allowedCounter = Counter.builder("iwm.quotas.tenant_requests.allowed")
                .description("Number of tenant-scoped requests accepted by the backend quota filter")
                .register(meterRegistry);
        this.rejectedCounter = Counter.builder("iwm.quotas.tenant_requests.rejected")
                .description("Number of tenant-scoped requests rejected by the backend quota filter")
                .register(meterRegistry);
        this.failOpenCounter = Counter.builder("iwm.quotas.tenant_requests.fail_open")
                .description("Number of quota enforcement failures bypassed in fail-open mode")
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }
        String serviceCode = resolveServiceCode(path);
        return resolveQuotaContext(exchange, serviceCode)
                .flatMap(context -> enforceQuota(exchange, chain, context).thenReturn(Boolean.TRUE))
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange).thenReturn(Boolean.TRUE)))
                .then();
    }

    private Mono<QuotaContext> resolveQuotaContext(ServerWebExchange exchange, String serviceCode) {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(securityContext -> securityContext.getAuthentication())
                .mapNotNull(authentication -> authentication instanceof ApiKeyAuthenticationToken token
                        ? new QuotaContext(token.tenantId(), token.clientId(), normalizeServiceCode(serviceCode))
                        : null)
                .filter(context -> context.tenantId() != null && context.clientId() != null && !context.clientId().isBlank())
                .switchIfEmpty(Mono.defer(() -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(TENANT_HEADER))
                        .flatMap(this::parseUuid)
                        .zipWith(Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(CLIENT_ID_HEADER)))
                        .map(tuple -> new QuotaContext(tuple.getT1(), tuple.getT2().trim(), normalizeServiceCode(serviceCode)))));
    }

    private Mono<Void> enforceQuota(ServerWebExchange exchange, WebFilterChain chain, QuotaContext context) {
        long windowSeconds = Math.max(1L, properties.getWindow().toSeconds());
        long bucket = Instant.now().getEpochSecond() / windowSeconds;
        String key = properties.getKeyPrefix()
                + ":" + context.tenantId()
                + ":" + context.clientId().toLowerCase(java.util.Locale.ROOT)
                + ":" + context.serviceCode()
                + ":" + bucket;
        Duration ttl = properties.getWindow().plusSeconds(5);

        return redisTemplate.opsForValue().increment(key)
                .flatMap(currentCount -> {
                    Mono<Boolean> ttlMono = currentCount != null && currentCount == 1L
                            ? redisTemplate.expire(key, ttl)
                            : Mono.just(Boolean.FALSE);
                    return ttlMono.thenReturn(currentCount);
                })
                .onErrorResume(error -> {
                    if (properties.isFailOpen()) {
                        failOpenCounter.increment();
                        return Mono.just(-1L);
                    }
                    return writeFailure(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                            "Tenant request quota service unavailable.",
                            "TENANT_REQUEST_QUOTA_UNAVAILABLE")
                            .then(Mono.empty());
                })
                .flatMap(currentCount -> {
                    if (currentCount != null && currentCount == -1L) {
                        return chain.filter(exchange);
                    }
                    long count = currentCount == null ? 0L : currentCount;
                    long remaining = Math.max(0L, properties.getLimit() - count);
                    HttpHeaders headers = exchange.getResponse().getHeaders();
                    headers.set("X-IWM-Quota-Limit", Long.toString(properties.getLimit()));
                    headers.set("X-IWM-Quota-Remaining", Long.toString(remaining));
                    headers.set("X-IWM-Quota-Window-Seconds", Long.toString(windowSeconds));
                    headers.set("X-IWM-Quota-Scope", "tenant-client-service");
                    headers.set("X-IWM-Quota-Client-Id", context.clientId());
                    headers.set("X-IWM-Quota-Service", context.serviceCode());
                    if (count > properties.getLimit()) {
                        rejectedCounter.increment();
                        headers.set("Retry-After", Long.toString(windowSeconds));
                        return writeFailure(exchange, HttpStatus.TOO_MANY_REQUESTS,
                                "Tenant request quota exceeded.",
                                "TENANT_REQUEST_QUOTA_EXCEEDED");
                    }
                    allowedCounter.increment();
                    return chain.filter(exchange);
                });
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

    private Mono<UUID> parseUuid(String value) {
        try {
            return Mono.just(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Mono.empty();
        }
    }

    private String resolveServiceCode(String path) {
        String mappedCode = routeResolver.resolveClientApplicationServiceCode(path);
        return mappedCode == null ? properties.getCoreServiceCode() : mappedCode;
    }

    private String normalizeServiceCode(String serviceCode) {
        if (serviceCode == null || serviceCode.isBlank()) {
            return properties.getCoreServiceCode();
        }
        return serviceCode.trim().replace('-', '_').replace(' ', '_').toUpperCase(java.util.Locale.ROOT);
    }

    private record QuotaContext(UUID tenantId, String clientId, String serviceCode) {
    }
}
