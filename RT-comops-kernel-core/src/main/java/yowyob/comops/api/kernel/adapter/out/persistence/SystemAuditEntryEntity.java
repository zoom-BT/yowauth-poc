package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "kernel", name = "system_audit_entry")
public record SystemAuditEntryEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        UUID organizationId,
        UUID actorUserId,
        String action,
        String targetType,
        String targetId,
        String payloadSummary,
        String requestId,
        String clientApplicationId,
        String remoteIp,
        String httpMethod,
        String httpPath,
        String integrityHash) implements PersistableEntity {
}
