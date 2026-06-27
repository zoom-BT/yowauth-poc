package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "kernel", name = "outbox_event")
public record OutboxEventEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        UUID organizationId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        String payload,
        String status,
        Integer attemptCount,
        Instant lastAttemptAt,
        Instant nextAttemptAt,
        String lastError,
        Instant deadLetteredAt,
        Instant publishedAt,
        UUID actorUserId,
        String clientApplicationId,
        String requestId) implements PersistableEntity {
}
