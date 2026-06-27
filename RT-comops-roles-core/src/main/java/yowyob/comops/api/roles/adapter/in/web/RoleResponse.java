package yowyob.comops.api.roles.adapter.in.web;

import yowyob.comops.api.roles.domain.model.Role;
import java.util.Set;
import java.util.UUID;

public record RoleResponse(UUID id, UUID tenantId, String code, String name, String scopeType,
        Set<String> permissions) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(role.id(), role.tenantId(), role.code(), role.name(), role.scopeType().name(),
                role.permissions());
    }
}
