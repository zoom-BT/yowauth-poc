package yowyob.comops.api.file.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "file", name = "document_governance_policy")
public record DocumentGovernancePolicyEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        UUID organizationId,
        UUID agencyId,
        String targetType,
        String documentCategory,
        boolean mandatory,
        boolean approvalRequired,
        Integer expiryDays,
        String reviewerResponsibilityType) implements PersistableEntity {
}
