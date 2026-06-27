package yowyob.comops.api.kernel.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.UUID;

public final class SystemAuditEntry extends BaseEntity {

    private final UUID organizationId;
    private final UUID actorUserId;
    private final String action;
    private final String targetType;
    private final String targetId;
    private final String payloadSummary;
    private final String requestId;
    private final String clientApplicationId;
    private final String remoteIp;
    private final String httpMethod;
    private final String httpPath;
    private final String integrityHash;

    private SystemAuditEntry(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID organizationId,
            UUID actorUserId, String action, String targetType, String targetId, String payloadSummary,
            String requestId, String clientApplicationId, String remoteIp, String httpMethod, String httpPath,
            String integrityHash) {
        super(id, tenantId, createdAt, updatedAt);
        this.organizationId = organizationId;
        this.actorUserId = actorUserId;
        this.action = requireText(action, "action");
        this.targetType = requireText(targetType, "targetType");
        this.targetId = requireText(targetId, "targetId");
        this.payloadSummary = requireText(payloadSummary, "payloadSummary");
        this.requestId = trimToNull(requestId);
        this.clientApplicationId = trimToNull(clientApplicationId);
        this.remoteIp = trimToNull(remoteIp);
        this.httpMethod = trimToNull(httpMethod);
        this.httpPath = trimToNull(httpPath);
        this.integrityHash = trimToNull(integrityHash);
    }

    public static SystemAuditEntry record(UUID tenantId, UUID organizationId, UUID actorUserId, String action,
            String targetType, String targetId, String payloadSummary) {
        return record(tenantId, organizationId, actorUserId, action, targetType, targetId, payloadSummary,
                null, null, null, null, null);
    }

    public static SystemAuditEntry record(UUID tenantId, UUID organizationId, UUID actorUserId, String action,
            String targetType, String targetId, String payloadSummary,
            String requestId, String clientApplicationId, String remoteIp, String httpMethod, String httpPath) {
        Instant now = Instant.now();
        return new SystemAuditEntry(UUID.randomUUID(), tenantId, now, now, organizationId, actorUserId, action,
                targetType, targetId, payloadSummary, requestId, clientApplicationId, remoteIp, httpMethod, httpPath,
                null);
    }

    public SystemAuditEntry withIntegrityHash(String integrityHash) {
        return new SystemAuditEntry(id(), tenantId(), createdAt(), updatedAt(), organizationId, actorUserId,
                action, targetType, targetId, payloadSummary, requestId, clientApplicationId, remoteIp,
                httpMethod, httpPath, integrityHash);
    }

    public static SystemAuditEntry rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID organizationId, UUID actorUserId, String action, String targetType, String targetId,
            String payloadSummary) {
        return rehydrate(id, tenantId, createdAt, updatedAt, organizationId, actorUserId, action,
                targetType, targetId, payloadSummary, null, null, null, null, null, null);
    }

    public static SystemAuditEntry rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID organizationId, UUID actorUserId, String action, String targetType, String targetId,
            String payloadSummary, String requestId, String clientApplicationId, String remoteIp,
            String httpMethod, String httpPath) {
        return rehydrate(id, tenantId, createdAt, updatedAt, organizationId, actorUserId, action,
                targetType, targetId, payloadSummary, requestId, clientApplicationId, remoteIp, httpMethod, httpPath,
                null);
    }

    public static SystemAuditEntry rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID organizationId, UUID actorUserId, String action, String targetType, String targetId,
            String payloadSummary, String requestId, String clientApplicationId, String remoteIp,
            String httpMethod, String httpPath, String integrityHash) {
        return new SystemAuditEntry(id, tenantId, createdAt, updatedAt, organizationId, actorUserId, action,
                targetType, targetId, payloadSummary, requestId, clientApplicationId, remoteIp, httpMethod, httpPath,
                integrityHash);
    }

    public UUID organizationId() { return organizationId; }
    public UUID actorUserId() { return actorUserId; }
    public String action() { return action; }
    public String targetType() { return targetType; }
    public String targetId() { return targetId; }
    public String payloadSummary() { return payloadSummary; }
    public String requestId() { return requestId; }
    public String clientApplicationId() { return clientApplicationId; }
    public String remoteIp() { return remoteIp; }
    public String httpMethod() { return httpMethod; }
    public String httpPath() { return httpPath; }
    public String integrityHash() { return integrityHash; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
