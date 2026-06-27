package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import yowyob.comops.api.kernel.application.port.out.OrganizationServiceRuntimeEntitlement;
import yowyob.comops.api.kernel.application.port.out.OrganizationServiceRuntimeEntitlementDirectory;
import yowyob.comops.api.kernel.config.OrganizationServiceRequestQuotaProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
public class OrganizationInterServiceCommunicationGuard {

    private final OrganizationServiceRuntimeEntitlementDirectory entitlementDirectory;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final OrganizationServiceRequestQuotaProperties quotaProperties;

    public OrganizationInterServiceCommunicationGuard(
            OrganizationServiceRuntimeEntitlementDirectory entitlementDirectory,
            ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
            OrganizationServiceRequestQuotaProperties quotaProperties) {
        this.entitlementDirectory = entitlementDirectory;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.quotaProperties = quotaProperties;
    }

    public Mono<Void> authorize(UUID tenantId, UUID organizationId, String sourceServiceCode, String targetServiceCode,
            String operation) {
        PlatformServiceCode targetService = PlatformServiceCode.from(targetServiceCode);
        Mono<Void> sourceCheck = sourceServiceCode == null || sourceServiceCode.isBlank()
                ? Mono.empty()
                : requireEffectiveService(tenantId, organizationId, PlatformServiceCode.from(sourceServiceCode),
                        operation, "source");
        return sourceCheck.then(requireEffectiveService(tenantId, organizationId, targetService, operation, "target"))
                .then(enforceQuota(tenantId, organizationId, targetService, operation));
    }

    private Mono<Void> requireEffectiveService(UUID tenantId, UUID organizationId, PlatformServiceCode service,
            String operation, String side) {
        return entitlementDirectory.resolveRuntimeEntitlement(tenantId, organizationId, service.code())
                .flatMap(entitlement -> entitlement.effective()
                        ? Mono.<Void>empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                message(operation, service, side, "is not subscribed for this organization"))));
    }

    private Mono<Void> enforceQuota(UUID tenantId, UUID organizationId, PlatformServiceCode targetService,
            String operation) {
        return entitlementDirectory.resolveRuntimeEntitlement(tenantId, organizationId, targetService.code())
                .flatMap(entitlement -> {
                    if (!quotaProperties.isEnabled()
                            || redisTemplate == null
                            || entitlement.requestQuotaLimit() == null
                            || entitlement.requestQuotaWindowSeconds() == null) {
                        return Mono.<Void>empty();
                    }
                    long windowSeconds = Math.max(1L, entitlement.requestQuotaWindowSeconds());
                    long bucket = Instant.now().getEpochSecond() / windowSeconds;
                    String key = quotaProperties.getKeyPrefix()
                            + ":" + tenantId
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
                                    if (count > entitlement.requestQuotaLimit()) {
                                        return Mono.<Void>error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                                message(operation, targetService, "target",
                                                        "quota exceeded for this organization")));
                                    }
                                    return Mono.<Void>empty();
                                }));
                            })
                            .onErrorResume(error -> quotaProperties.isFailOpen()
                                    ? Mono.<Void>empty()
                                    : Mono.<Void>error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                            message(operation, targetService, "target",
                                                    "quota service unavailable"))));
                });
    }

    private String message(String operation, PlatformServiceCode service, String side, String suffix) {
        String normalizedOperation = operation == null || operation.isBlank()
                ? "inter-service communication"
                : operation.trim();
        return normalizedOperation + " refused: " + side + " service " + service.code() + " " + suffix + ".";
    }
}
