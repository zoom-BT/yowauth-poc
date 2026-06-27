package yowyob.comops.api.roles.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.UUID;

public final class UserRoleAssignment extends BaseEntity {

    private final UUID userId;
    private final UUID roleId;
    private final RoleScopeType scopeType;
    private final UUID scopeId;
    private final String scope;

    private UserRoleAssignment(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID userId, UUID roleId,
            RoleScopeType scopeType, UUID scopeId, String scope) {
        super(id, tenantId, createdAt, updatedAt);
        this.userId = requireUuid(userId, "userId");
        this.roleId = requireUuid(roleId, "roleId");
        this.scopeType = scopeType == null ? RoleScopeType.TENANT : scopeType;
        this.scopeId = validateScopeId(this.scopeType, scopeId);
        this.scope = requireText(scope, "scope");
    }

    public static UserRoleAssignment assign(UUID tenantId, UUID userId, UUID roleId, String scope) {
        ScopeDescriptor descriptor = ScopeDescriptor.parse(scope);
        return assign(tenantId, userId, roleId, descriptor.scopeType(), descriptor.scopeId());
    }

    public static UserRoleAssignment assign(UUID tenantId, UUID userId, UUID roleId, RoleScopeType scopeType, UUID scopeId) {
        Instant now = Instant.now();
        return new UserRoleAssignment(UUID.randomUUID(), tenantId, now, now, userId, roleId,
                scopeType, scopeId, ScopeDescriptor.format(scopeType, scopeId));
    }

    public static UserRoleAssignment rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID userId,
            UUID roleId, String scopeType, UUID scopeId, String scope) {
        return new UserRoleAssignment(id, tenantId, createdAt, updatedAt, userId, roleId,
                RoleScopeType.from(scopeType), scopeId, scope == null || scope.isBlank()
                        ? ScopeDescriptor.format(RoleScopeType.from(scopeType), scopeId)
                        : scope);
    }

    public UUID userId() { return userId; }
    public UUID roleId() { return roleId; }
    public RoleScopeType scopeType() { return scopeType; }
    public UUID scopeId() { return scopeId; }
    public String scope() { return scope; }

    private static UUID requireUuid(UUID value, String field) { if (value == null) throw new IllegalArgumentException(field + " is required"); return value; }
    private static String requireText(String value, String field) { if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required"); return value.trim(); }

    private static UUID validateScopeId(RoleScopeType scopeType, UUID scopeId) {
        return switch (scopeType) {
            case SYSTEM, TENANT -> null;
            case ORGANIZATION, AGENCY -> requireUuid(scopeId, "scopeId");
        };
    }

    private record ScopeDescriptor(RoleScopeType scopeType, UUID scopeId) {

        private static ScopeDescriptor parse(String rawScope) {
            if (rawScope == null || rawScope.isBlank()) {
                return new ScopeDescriptor(RoleScopeType.TENANT, null);
            }
            String normalized = rawScope.trim();
            if ("GLOBAL".equalsIgnoreCase(normalized) || "TENANT".equalsIgnoreCase(normalized)) {
                return new ScopeDescriptor(RoleScopeType.TENANT, null);
            }
            if ("SYSTEM".equalsIgnoreCase(normalized)) {
                return new ScopeDescriptor(RoleScopeType.SYSTEM, null);
            }
            int separatorIndex = normalized.indexOf(':');
            if (separatorIndex <= 0 || separatorIndex == normalized.length() - 1) {
                throw new IllegalArgumentException("Invalid scope value: " + rawScope);
            }
            RoleScopeType parsedType = RoleScopeType.from(normalized.substring(0, separatorIndex));
            UUID parsedScopeId = UUID.fromString(normalized.substring(separatorIndex + 1));
            return new ScopeDescriptor(parsedType, parsedScopeId);
        }

        private static String format(RoleScopeType scopeType, UUID scopeId) {
            return switch (scopeType) {
                case SYSTEM -> RoleScopeType.SYSTEM.legacyPrefix();
                case TENANT -> RoleScopeType.TENANT.legacyPrefix();
                case ORGANIZATION, AGENCY -> scopeType.legacyPrefix() + ":" + requireUuid(scopeId, "scopeId");
            };
        }
    }
}
