package yowyob.comops.api.roles.application.port.in;

import java.util.UUID;

public record AssignRoleToUserCommand(UUID tenantId, UUID userId, UUID roleId, String scopeType, UUID scopeId,
        String scope) {

    public AssignRoleToUserCommand(UUID tenantId, UUID userId, UUID roleId, String scope) {
        this(tenantId, userId, roleId, null, null, scope);
    }
}
