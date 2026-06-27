package yowyob.comops.api.file.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class DocumentReview extends BaseEntity {

    private final UUID organizationId;
    private final UUID documentLinkId;
    private final UUID reviewerUserId;
    private final String reviewStatus;
    private final Instant reviewedAt;
    private final Instant expiresAt;
    private final String notes;

    private DocumentReview(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID organizationId,
            UUID documentLinkId, UUID reviewerUserId, String reviewStatus, Instant reviewedAt, Instant expiresAt,
            String notes) {
        super(id, tenantId, createdAt, updatedAt);
        this.organizationId = requireUuid(organizationId, "organizationId");
        this.documentLinkId = requireUuid(documentLinkId, "documentLinkId");
        this.reviewerUserId = requireUuid(reviewerUserId, "reviewerUserId");
        this.reviewStatus = normalizeCode(reviewStatus, "reviewStatus");
        this.reviewedAt = reviewedAt == null ? createdAt : reviewedAt;
        this.expiresAt = expiresAt;
        this.notes = notes == null || notes.isBlank() ? null : notes.trim();
    }

    public static DocumentReview review(UUID tenantId, UUID organizationId, UUID documentLinkId,
            UUID reviewerUserId, String reviewStatus, Instant expiresAt, String notes) {
        Instant now = Instant.now();
        return new DocumentReview(UUID.randomUUID(), tenantId, now, now, organizationId, documentLinkId,
                reviewerUserId, reviewStatus, now, expiresAt, notes);
    }

    public static DocumentReview rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID organizationId, UUID documentLinkId, UUID reviewerUserId, String reviewStatus,
            Instant reviewedAt, Instant expiresAt, String notes) {
        return new DocumentReview(id, tenantId, createdAt, updatedAt, organizationId, documentLinkId,
                reviewerUserId, reviewStatus, reviewedAt, expiresAt, notes);
    }

    public UUID organizationId() { return organizationId; }
    public UUID documentLinkId() { return documentLinkId; }
    public UUID reviewerUserId() { return reviewerUserId; }
    public String reviewStatus() { return reviewStatus; }
    public Instant reviewedAt() { return reviewedAt; }
    public Instant expiresAt() { return expiresAt; }
    public String notes() { return notes; }

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
