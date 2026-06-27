package yowyob.comops.api.common.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Minimal immutable base entity shared by domain modules.
 */
public abstract class BaseEntity {

    private final UUID id;
    private final UUID tenantId;
    private final Instant createdAt;
    private final Instant updatedAt;

    protected BaseEntity(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
