package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.kernel.application.port.in.GetOutboxObservabilityUseCase;
import yowyob.comops.api.kernel.application.port.in.GetProjectionObservabilityUseCase;
import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.application.port.out.OutboxEventRepository;
import yowyob.comops.api.kernel.config.ElasticsearchSearchProperties;
import yowyob.comops.api.kernel.config.OutboxConsumersProperties;
import yowyob.comops.api.kernel.config.OutboxRelayProperties;
import yowyob.comops.api.kernel.config.RedisPermissionCacheProperties;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;
import yowyob.comops.api.kernel.domain.model.DomainEventProjection;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import yowyob.comops.api.kernel.domain.model.OutboxEventStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class KernelObservabilityApplicationService
        implements GetOutboxObservabilityUseCase, GetProjectionObservabilityUseCase {

    private final OutboxEventRepository outboxEventRepository;
    private final DomainEventProjectionRepository domainEventProjectionRepository;
    private final String persistenceMode;
    private final String outboxDeliveryType;
    private final OutboxRelayProperties outboxRelayProperties;
    private final OutboxConsumersProperties outboxConsumersProperties;
    private final RedisPermissionCacheProperties redisPermissionCacheProperties;
    private final ElasticsearchSearchProperties elasticsearchSearchProperties;
    private final SecurityRuntimeProperties securityRuntimeProperties;

    public KernelObservabilityApplicationService(OutboxEventRepository outboxEventRepository,
            DomainEventProjectionRepository domainEventProjectionRepository,
            @Value("${iwm.persistence.mode:r2dbc}") String persistenceMode,
            @Value("${iwm.outbox.delivery.type:kafka}") String outboxDeliveryType,
            OutboxRelayProperties outboxRelayProperties,
            OutboxConsumersProperties outboxConsumersProperties,
            RedisPermissionCacheProperties redisPermissionCacheProperties,
            ElasticsearchSearchProperties elasticsearchSearchProperties,
            SecurityRuntimeProperties securityRuntimeProperties) {
        this.outboxEventRepository = outboxEventRepository;
        this.domainEventProjectionRepository = domainEventProjectionRepository;
        this.persistenceMode = persistenceMode;
        this.outboxDeliveryType = outboxDeliveryType;
        this.outboxRelayProperties = outboxRelayProperties;
        this.outboxConsumersProperties = outboxConsumersProperties;
        this.redisPermissionCacheProperties = redisPermissionCacheProperties;
        this.elasticsearchSearchProperties = elasticsearchSearchProperties;
        this.securityRuntimeProperties = securityRuntimeProperties;
    }

    @Override
    public Flux<OutboxEvent> listEvents(UUID tenantId, OutboxEventStatus status, int limit) {
        if (status == null) {
            return outboxEventRepository.findByTenantId(tenantId)
                    .take(limit <= 0 ? Long.MAX_VALUE : limit);
        }
        return outboxEventRepository.findByTenantIdAndStatus(tenantId, status, limit);
    }

    @Override
    public Mono<OutboxEventSummaryView> summarizeOutbox(UUID tenantId) {
        return Mono.zip(
                        outboxEventRepository.countByTenantIdAndStatus(tenantId, OutboxEventStatus.PENDING),
                        outboxEventRepository.countByTenantIdAndStatus(tenantId, OutboxEventStatus.PUBLISHED),
                        outboxEventRepository.countByTenantIdAndStatus(tenantId, OutboxEventStatus.DEAD_LETTER))
                .map(tuple -> new OutboxEventSummaryView(tenantId.toString(), tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }

    @Override
    public Flux<DomainEventProjection> listProjections(UUID tenantId, String domainType, int limit) {
        if (domainType == null || domainType.isBlank()) {
            return domainEventProjectionRepository.findByTenantId(tenantId)
                    .take(limit <= 0 ? Long.MAX_VALUE : limit);
        }
        return domainEventProjectionRepository.findByTenantIdAndDomainType(tenantId, domainType, limit);
    }

    @Override
    public Mono<DomainProjectionSummaryView> summarizeProjections(UUID tenantId) {
        return domainEventProjectionRepository.findByTenantId(tenantId)
                .collect(() -> new LinkedHashMap<String, Long>(), (counts, projection) ->
                        counts.merge(projection.domainType(), 1L, Long::sum))
                .flatMap(counts -> domainEventProjectionRepository.countByTenantId(tenantId)
                        .map(total -> new DomainProjectionSummaryView(tenantId.toString(), total, Map.copyOf(counts))));
    }

    public Mono<OperationalRuntimeView> runtime() {
        return Mono.just(new OperationalRuntimeView(
                persistenceMode,
                outboxDeliveryType,
                outboxRelayProperties.isEnabled(),
                outboxConsumersProperties.getMode(),
                redisPermissionCacheProperties.isEnabled(),
                elasticsearchSearchProperties.isEnabled(),
                securityRuntimeProperties.getClientApplications().getBootstrap().isEnabled()));
    }
}
