package yowyob.comops.api.auth.adapter.in.web;

import java.util.UUID;

public record ContextualLoginResponse(
        UUID selectedTenantId,
        UUID selectedOrganizationId,
        LoginResponse session) {

    public static ContextualLoginResponse from(UUID tenantId, UUID organizationId, LoginResponse session) {
        return new ContextualLoginResponse(tenantId, organizationId, session);
    }
}
