package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "integration", name = "domain_event_projection")
public record DomainEventProjectionEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        UUID sourceEventId,
        UUID organizationId,
        String domainType,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        String businessKey,
        String lifecycleStatus,
        Instant occurredAt,
        String payload) implements PersistableEntity {
}
