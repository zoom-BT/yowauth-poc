package yowyob.comops.api.roles.application.port.in;

import java.util.Set;
import java.util.UUID;

public record CreateRoleCommand(UUID tenantId, String code, String name, String scopeType, Set<String> permissions) {

    public CreateRoleCommand(UUID tenantId, String code, String name, Set<String> permissions) {
        this(tenantId, code, name, null, permissions);
    }
}
