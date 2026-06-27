package yowyob.comops.api.file.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "file_core", name = "document_link")
public record DocumentLinkEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        UUID organizationId,
        String targetType,
        UUID targetId,
        UUID fileId,
        String documentCategory,
        String label,
        UUID attachedByUserId,
        Instant attachedAt) implements PersistableEntity {
}
