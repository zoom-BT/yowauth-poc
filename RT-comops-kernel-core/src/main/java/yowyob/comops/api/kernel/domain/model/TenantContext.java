package yowyob.comops.api.kernel.domain.model;

import java.util.UUID;

public record TenantContext(UUID tenantId, UUID organizationId, UUID agencyId, UUID userId, UUID actorId) {

    public TenantContext {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
    }
}
