package yowyob.comops.api.kernel.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.kernel.application.port.out.OutboxEventRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import yowyob.comops.api.kernel.domain.model.OutboxEventStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class OutboxEventR2dbcRepositoryAdapter implements OutboxEventRepository {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() { };

    private final OutboxEventSpringDataRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxEventR2dbcRepositoryAdapter(OutboxEventSpringDataRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<OutboxEvent> save(OutboxEvent event) {
        return repository.save(toEntity(event)).map(this::toDomain);
    }

    @Override
    public Flux<OutboxEvent> findByTenantId(UUID tenantId) {
        return repository.findAllByTenantIdOrderByCreatedAtAsc(tenantId).map(this::toDomain);
    }

    @Override
    public Flux<OutboxEvent> findByTenantIdAndStatus(UUID tenantId, OutboxEventStatus status, int limit) {
        return repository.findAllByTenantIdAndStatusOrderByCreatedAtAsc(tenantId, status.name())
                .take(limit <= 0 ? Long.MAX_VALUE : limit)
                .map(this::toDomain);
    }

    @Override
    public Flux<OutboxEvent> findByStatus(OutboxEventStatus status, int limit) {
        return repository.findAllByStatusOrderByOccurredAtAsc(status.name())
                .take(limit <= 0 ? Long.MAX_VALUE : limit)
                .map(this::toDomain);
    }

    @Override
    public Flux<OutboxEvent> findReadyForRelay(Instant asOf, int limit) {
        Instant effectiveAsOf = asOf == null ? Instant.now() : asOf;
        return repository.findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING.name(), effectiveAsOf)
                .take(limit <= 0 ? Long.MAX_VALUE : limit)
                .map(this::toDomain);
    }

    @Override
    public Mono<Long> countByStatus(OutboxEventStatus status) {
        return repository.countAllByStatus(status.name());
    }

    @Override
    public Mono<Long> countByTenantIdAndStatus(UUID tenantId, OutboxEventStatus status) {
        return repository.countAllByTenantIdAndStatus(tenantId, status.name());
    }

    @Override
    public Mono<Instant> findOldestPendingOccurredAt() {
        return repository.findFirstByStatusOrderByOccurredAtAsc(OutboxEventStatus.PENDING.name())
                .map(OutboxEventEntity::occurredAt);
    }

    @Override
    public Mono<Instant> findLatestPublishedAt() {
        return repository.findFirstByStatusOrderByPublishedAtDesc(OutboxEventStatus.PUBLISHED.name())
                .map(OutboxEventEntity::publishedAt);
    }

    private OutboxEventEntity toEntity(OutboxEvent event) {
        try {
            return new OutboxEventEntity(event.id(), event.tenantId(), event.createdAt(), event.updatedAt(),
                    event.organizationId(), event.eventType(), event.aggregateType(), event.aggregateId(),
                    event.occurredAt(), objectMapper.writeValueAsString(event.payload()), event.status().name(),
                    event.attemptCount(), event.lastAttemptAt(), event.nextAttemptAt(), event.lastError(),
                    event.deadLetteredAt(), event.publishedAt(),
                    event.actorUserId(), event.clientApplicationId(), event.requestId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize outbox event payload", exception);
        }
    }

    private OutboxEvent toDomain(OutboxEventEntity entity) {
        try {
            return OutboxEvent.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(), entity.updatedAt(),
                    entity.organizationId(), entity.eventType(), entity.aggregateType(), entity.aggregateId(),
                    entity.occurredAt(), objectMapper.readValue(entity.payload(), PAYLOAD_TYPE),
                    OutboxEventStatus.valueOf(entity.status()), entity.attemptCount() == null ? 0 : entity.attemptCount(),
                    entity.lastAttemptAt(), entity.nextAttemptAt(), entity.lastError(), entity.deadLetteredAt(),
                    entity.publishedAt(),
                    entity.actorUserId(), entity.clientApplicationId(), entity.requestId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize outbox event payload", exception);
        }
    }
}
