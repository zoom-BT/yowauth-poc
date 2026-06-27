package yowyob.comops.api.file.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "file", name = "document_review")
public record DocumentReviewEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        UUID organizationId,
        UUID documentLinkId,
        UUID reviewerUserId,
        String reviewStatus,
        Instant reviewedAt,
        Instant expiresAt,
        String notes) implements PersistableEntity {
}
