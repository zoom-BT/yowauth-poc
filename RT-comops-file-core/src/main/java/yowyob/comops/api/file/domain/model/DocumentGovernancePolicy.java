package yowyob.comops.api.file.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class DocumentGovernancePolicy extends BaseEntity {

    private final UUID organizationId;
    private final UUID agencyId;
    private final String targetType;
    private final String documentCategory;
    private final boolean mandatory;
    private final boolean approvalRequired;
    private final Integer expiryDays;
    private final String reviewerResponsibilityType;

    private DocumentGovernancePolicy(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID organizationId, UUID agencyId, String targetType, String documentCategory, boolean mandatory,
            boolean approvalRequired, Integer expiryDays, String reviewerResponsibilityType) {
        super(id, tenantId, createdAt, updatedAt);
        this.organizationId = requireUuid(organizationId, "organizationId");
        this.agencyId = agencyId;
        this.targetType = normalizeCode(targetType, "targetType");
        this.documentCategory = normalizeCode(documentCategory, "documentCategory");
        this.mandatory = mandatory;
        this.approvalRequired = approvalRequired;
        if (expiryDays != null && expiryDays < 0) {
            throw new IllegalArgumentException("expiryDays must be >= 0");
        }
        this.expiryDays = expiryDays;
        this.reviewerResponsibilityType = reviewerResponsibilityType == null || reviewerResponsibilityType.isBlank()
                ? null : reviewerResponsibilityType.trim().toUpperCase(Locale.ROOT);
    }

    public static DocumentGovernancePolicy create(UUID tenantId, UUID organizationId, UUID agencyId,
            String targetType, String documentCategory, boolean mandatory, boolean approvalRequired,
            Integer expiryDays, String reviewerResponsibilityType) {
        Instant now = Instant.now();
        return new DocumentGovernancePolicy(UUID.randomUUID(), tenantId, now, now, organizationId, agencyId,
                targetType, documentCategory, mandatory, approvalRequired, expiryDays,
                reviewerResponsibilityType);
    }

    public static DocumentGovernancePolicy rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID organizationId, UUID agencyId, String targetType, String documentCategory, boolean mandatory,
            boolean approvalRequired, Integer expiryDays, String reviewerResponsibilityType) {
        return new DocumentGovernancePolicy(id, tenantId, createdAt, updatedAt, organizationId, agencyId,
                targetType, documentCategory, mandatory, approvalRequired, expiryDays,
                reviewerResponsibilityType);
    }

    public DocumentGovernancePolicy update(boolean mandatory, boolean approvalRequired, Integer expiryDays,
            String reviewerResponsibilityType) {
        return new DocumentGovernancePolicy(id(), tenantId(), createdAt(), Instant.now(), organizationId, agencyId,
                targetType, documentCategory, mandatory, approvalRequired, expiryDays, reviewerResponsibilityType);
    }

    public UUID organizationId() { return organizationId; }
    public UUID agencyId() { return agencyId; }
    public String targetType() { return targetType; }
    public String documentCategory() { return documentCategory; }
    public boolean mandatory() { return mandatory; }
    public boolean approvalRequired() { return approvalRequired; }
    public Integer expiryDays() { return expiryDays; }
    public String reviewerResponsibilityType() { return reviewerResponsibilityType; }

    private static UUID requireUuid(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String normalizeCode(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
