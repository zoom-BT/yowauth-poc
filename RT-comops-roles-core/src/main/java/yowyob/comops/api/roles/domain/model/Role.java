package yowyob.comops.api.roles.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Role extends BaseEntity {

    private final String code;
    private final String name;
    private final RoleScopeType scopeType;
    private final Set<String> permissions;

    private Role(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, String code, String name,
            RoleScopeType scopeType, Set<String> permissions) {
        super(id, tenantId, createdAt, updatedAt);
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        this.scopeType = scopeType == null ? RoleScopeType.TENANT : scopeType;
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("permissions are required");
        }
        this.permissions = Set.copyOf(new LinkedHashSet<>(permissions));
    }

    public static Role create(UUID tenantId, String code, String name, RoleScopeType scopeType, Set<String> permissions) {
        Instant now = Instant.now();
        return new Role(UUID.randomUUID(), tenantId, now, now, code, name, scopeType, permissions);
    }

    public static Role rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, String code, String name,
            RoleScopeType scopeType, Set<String> permissions) {
        return new Role(id, tenantId, createdAt, updatedAt, code, name, scopeType, permissions);
    }

    public Role rename(String updatedName) {
        return new Role(id(), tenantId(), createdAt(), Instant.now(), code, updatedName, scopeType, permissions);
    }

    public Role replacePermissions(Set<String> updatedPermissions) {
        return new Role(id(), tenantId(), createdAt(), Instant.now(), code, name, scopeType, updatedPermissions);
    }

    public String code() { return code; }
    public String name() { return name; }
    public RoleScopeType scopeType() { return scopeType; }
    public Set<String> permissions() { return permissions; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
