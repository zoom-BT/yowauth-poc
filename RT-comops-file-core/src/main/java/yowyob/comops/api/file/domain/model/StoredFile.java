package yowyob.comops.api.file.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.UUID;

public final class StoredFile extends BaseEntity {

    private final UUID organizationId;
    private final UUID uploadedByUserId;
    private final String fileName;
    private final String contentType;
    private final long size;
    private final String storagePath;
    private final String documentType;
    private final FileAnalysisStatus analysisStatus;
    private final String analysisReason;
    private final Instant analyzedAt;

    private StoredFile(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID organizationId,
            UUID uploadedByUserId, String fileName, String contentType, long size, String storagePath,
            String documentType, FileAnalysisStatus analysisStatus, String analysisReason, Instant analyzedAt) {
        super(id, tenantId, createdAt, updatedAt);
        this.organizationId = organizationId;
        this.uploadedByUserId = uploadedByUserId;
        this.fileName = requireText(fileName, "fileName");
        this.contentType = requireText(contentType, "contentType");
        this.size = Math.max(size, 0L);
        this.storagePath = requireText(storagePath, "storagePath");
        this.documentType = documentType;
        this.analysisStatus = analysisStatus == null ? FileAnalysisStatus.PENDING : analysisStatus;
        this.analysisReason = analysisReason;
        this.analyzedAt = analyzedAt;
    }

    /** Crée un fichier en quarantaine (PENDING) en attente du verdict d'analyse. */
    public static StoredFile create(UUID tenantId, UUID organizationId, UUID uploadedByUserId, String fileName,
            String contentType, long size, String storagePath, String documentType) {
        Instant now = Instant.now();
        return new StoredFile(UUID.randomUUID(), tenantId, now, now, organizationId, uploadedByUserId, fileName,
                contentType, size, storagePath, documentType, FileAnalysisStatus.PENDING, null, null);
    }

    /**
     * Crée un fichier déjà ACCEPTED (téléchargeable), utilisé quand le fichier ne nécessite pas
     * d'analyse KYC. createdAt == updatedAt pour rester "new" côté persistance (INSERT).
     */
    public static StoredFile createAccepted(UUID tenantId, UUID organizationId, UUID uploadedByUserId, String fileName,
            String contentType, long size, String storagePath, String documentType) {
        Instant now = Instant.now();
        return new StoredFile(UUID.randomUUID(), tenantId, now, now, organizationId, uploadedByUserId, fileName,
                contentType, size, storagePath, documentType, FileAnalysisStatus.ACCEPTED, null, now);
    }

    public static StoredFile rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID organizationId, UUID uploadedByUserId, String fileName, String contentType, long size,
            String storagePath, String documentType, FileAnalysisStatus analysisStatus, String analysisReason,
            Instant analyzedAt) {
        return new StoredFile(id, tenantId, createdAt, updatedAt, organizationId, uploadedByUserId, fileName,
                contentType, size, storagePath, documentType, analysisStatus, analysisReason, analyzedAt);
    }

    /** Returns a copy cleared by the analysis service (downloadable). */
    public StoredFile markAccepted() {
        return new StoredFile(id(), tenantId(), createdAt(), Instant.now(), organizationId, uploadedByUserId, fileName,
                contentType, size, storagePath, documentType, FileAnalysisStatus.ACCEPTED, null, Instant.now());
    }

    /** Returns a copy flagged by the analysis service (kept in quarantine). */
    public StoredFile markRejected(String reason) {
        return new StoredFile(id(), tenantId(), createdAt(), Instant.now(), organizationId, uploadedByUserId, fileName,
                contentType, size, storagePath, documentType, FileAnalysisStatus.REJECTED, reason, Instant.now());
    }

    public UUID organizationId() { return organizationId; }
    public UUID uploadedByUserId() { return uploadedByUserId; }
    public String fileName() { return fileName; }
    public String contentType() { return contentType; }
    public long size() { return size; }
    public String storagePath() { return storagePath; }
    public String documentType() { return documentType; }
    public FileAnalysisStatus analysisStatus() { return analysisStatus; }
    public String analysisReason() { return analysisReason; }
    public Instant analyzedAt() { return analyzedAt; }

    public boolean isAccepted() {
        return analysisStatus == FileAnalysisStatus.ACCEPTED;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
