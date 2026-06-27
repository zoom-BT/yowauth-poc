package yowyob.comops.api.kernel.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.domain.model.DomainEventProjection;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class DomainEventProjectionR2dbcRepositoryAdapter implements DomainEventProjectionRepository {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() { };

    private final DomainEventProjectionSpringDataRepository repository;
    private final ObjectMapper objectMapper;

    public DomainEventProjectionR2dbcRepositoryAdapter(DomainEventProjectionSpringDataRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<DomainEventProjection> save(DomainEventProjection projection) {
        return repository.save(toEntity(projection)).map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsBySourceEventId(UUID sourceEventId) {
        return repository.existsBySourceEventId(sourceEventId);
    }

    @Override
    public Flux<DomainEventProjection> findByTenantId(UUID tenantId) {
        return repository.findAllByTenantIdOrderByCreatedAtAsc(tenantId).map(this::toDomain);
    }

    @Override
    public Flux<DomainEventProjection> findByTenantIdAndDomainType(UUID tenantId, String domainType, int limit) {
        return repository.findAllByTenantIdAndDomainTypeOrderByCreatedAtAsc(tenantId, domainType == null ? null : domainType.trim().toUpperCase())
                .take(limit <= 0 ? Long.MAX_VALUE : limit)
                .map(this::toDomain);
    }

    @Override
    public Mono<Long> countByTenantId(UUID tenantId) {
        return repository.countAllByTenantId(tenantId);
    }

    @Override
    public Mono<Long> countByTenantIdAndDomainType(UUID tenantId, String domainType) {
        return repository.countAllByTenantIdAndDomainType(tenantId, domainType == null ? null : domainType.trim().toUpperCase());
    }

    @Override
    public Mono<Long> countAll() {
        return repository.count();
    }

    @Override
    public Mono<java.time.Instant> findLatestOccurredAt() {
        return repository.findFirstByOrderByOccurredAtDesc()
                .map(DomainEventProjectionEntity::occurredAt);
    }

    @Override
    public Mono<java.time.Instant> findLatestCreatedAt() {
        return repository.findFirstByOrderByCreatedAtDesc()
                .map(DomainEventProjectionEntity::createdAt);
    }

    private DomainEventProjectionEntity toEntity(DomainEventProjection projection) {
        try {
            return new DomainEventProjectionEntity(projection.id(), projection.tenantId(), projection.createdAt(),
                    projection.updatedAt(), projection.sourceEventId(), projection.organizationId(),
                    projection.domainType(), projection.eventType(), projection.aggregateType(),
                    projection.aggregateId(), projection.businessKey(),
                    projection.lifecycleStatus(), projection.occurredAt(),
                    objectMapper.writeValueAsString(projection.payload()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize domain event projection payload", exception);
        }
    }

    private DomainEventProjection toDomain(DomainEventProjectionEntity entity) {
        try {
            return DomainEventProjection.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(),
                    entity.updatedAt(), entity.sourceEventId(), entity.organizationId(), entity.domainType(),
                    entity.eventType(), entity.aggregateType(), entity.aggregateId(), entity.businessKey(),
                    entity.lifecycleStatus(), entity.occurredAt(),
                    objectMapper.readValue(entity.payload(), PAYLOAD_TYPE));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize domain event projection payload", exception);
        }
    }
}
