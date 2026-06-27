package yowyob.comops.api.roles.application.service;

import yowyob.comops.api.kernel.application.port.out.ReactivePermissionCache;
import yowyob.comops.api.kernel.application.port.out.ReactivePermissionResolver;
import yowyob.comops.api.roles.application.port.out.RoleRepository;
import yowyob.comops.api.roles.application.port.out.UserRoleAssignmentRepository;
import yowyob.comops.api.roles.domain.model.RoleScopeType;
import yowyob.comops.api.roles.domain.model.UserRoleAssignment;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Primary
public class RolesPermissionResolver implements ReactivePermissionResolver {

    private final UserRoleAssignmentRepository assignmentRepository;
    private final RoleRepository roleRepository;
    private final Optional<ReactivePermissionCache> permissionCache;

    public RolesPermissionResolver(UserRoleAssignmentRepository assignmentRepository, RoleRepository roleRepository,
            Optional<ReactivePermissionCache> permissionCache) {
        this.assignmentRepository = assignmentRepository;
        this.roleRepository = roleRepository;
        this.permissionCache = permissionCache;
    }

    @Override
    public Mono<Set<String>> resolvePermissions(UUID tenantId, UUID userId) {
        Mono<Set<String>> directLookup = assignmentRepository.findByTenantIdAndUserId(tenantId, userId)
                .flatMap(assignment -> roleRepository.findById(tenantId, assignment.roleId())
                        .flatMapIterable(role -> mapAuthorities(assignment, role.code(), role.permissions())))
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .collectList()
                .map(permissions -> Set.copyOf(new LinkedHashSet<>(permissions)));
        if (permissionCache.isEmpty()) {
            return directLookup;
        }
        ReactivePermissionCache cache = permissionCache.get();
        return cache.get(tenantId, userId)
                .switchIfEmpty(directLookup.flatMap(permissions -> cache.put(tenantId, userId, permissions)
                        .thenReturn(permissions)));
    }

    private Set<String> mapAuthorities(UserRoleAssignment assignment, String roleCode, Set<String> rolePermissions) {
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        RoleScopeType scopeType = assignment.scopeType();
        String scopeAuthority = scopedAuthoritySuffix(scopeType, assignment.scopeId());
        if (roleCode != null && !roleCode.isBlank()) {
            String roleAuthority = "ROLE_" + roleCode.trim().toUpperCase();
            if (scopeType == RoleScopeType.SYSTEM || scopeType == RoleScopeType.TENANT) {
                authorities.add(roleAuthority);
            }
            authorities.add(roleAuthority + scopeAuthority);
        }
        rolePermissions.stream()
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .forEach(permission -> {
                    if (scopeType == RoleScopeType.SYSTEM || scopeType == RoleScopeType.TENANT) {
                        authorities.add(permission);
                    }
                    authorities.add(permission + scopeAuthority);
                });
        return Set.copyOf(authorities);
    }

    private String scopedAuthoritySuffix(RoleScopeType scopeType, UUID scopeId) {
        return switch (scopeType) {
            case SYSTEM -> "#SYSTEM";
            case TENANT -> "#TENANT";
            case ORGANIZATION -> "#ORGANIZATION:" + scopeId;
            case AGENCY -> "#AGENCY:" + scopeId;
        };
    }
}
