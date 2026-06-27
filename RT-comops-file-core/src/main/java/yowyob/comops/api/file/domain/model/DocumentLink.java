package yowyob.comops.api.file.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.UUID;

public final class DocumentLink extends BaseEntity {

    private final UUID organizationId;
    private final String targetType;
    private final UUID targetId;
    private final UUID fileId;
    private final String documentCategory;
    private final String label;
    private final UUID attachedByUserId;
    private final Instant attachedAt;

    private DocumentLink(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID organizationId,
            String targetType, UUID targetId, UUID fileId, String documentCategory, String label,
            UUID attachedByUserId, Instant attachedAt) {
        super(id, tenantId, createdAt, updatedAt);
        this.organizationId = requireUuid(organizationId, "organizationId");
        this.targetType = requireText(targetType, "targetType").toUpperCase();
        this.targetId = requireUuid(targetId, "targetId");
        this.fileId = requireUuid(fileId, "fileId");
        this.documentCategory = requireText(documentCategory, "documentCategory").toUpperCase();
        this.label = normalize(label);
        this.attachedByUserId = attachedByUserId;
        this.attachedAt = attachedAt == null ? createdAt : attachedAt;
    }

    public static DocumentLink attach(UUID tenantId, UUID organizationId, String targetType, UUID targetId,
            UUID fileId, String documentCategory, String label, UUID attachedByUserId) {
        Instant now = Instant.now();
        return new DocumentLink(UUID.randomUUID(), tenantId, now, now, organizationId, targetType, targetId, fileId,
                documentCategory, label, attachedByUserId, now);
    }

    public static DocumentLink rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID organizationId, String targetType, UUID targetId, UUID fileId, String documentCategory, String label,
            UUID attachedByUserId, Instant attachedAt) {
        return new DocumentLink(id, tenantId, createdAt, updatedAt, organizationId, targetType, targetId, fileId,
                documentCategory, label, attachedByUserId, attachedAt);
    }

    public UUID organizationId() { return organizationId; }
    public String targetType() { return targetType; }
    public UUID targetId() { return targetId; }
    public UUID fileId() { return fileId; }
    public String documentCategory() { return documentCategory; }
    public String label() { return label; }
    public UUID attachedByUserId() { return attachedByUserId; }
    public Instant attachedAt() { return attachedAt; }

    private static UUID requireUuid(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
