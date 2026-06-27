package yowyob.comops.api.roles.application.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import yowyob.comops.api.kernel.application.port.out.TenantOwnerRoleProvisioner;
import yowyob.comops.api.roles.application.port.out.RoleRepository;
import yowyob.comops.api.roles.application.port.out.UserRoleAssignmentRepository;
import yowyob.comops.api.roles.domain.model.Role;
import yowyob.comops.api.roles.domain.model.RoleScopeType;
import yowyob.comops.api.roles.domain.model.UserRoleAssignment;

/**
 * Grants the creator of a brand-new tenant a TENANT-scoped owner role so they can
 * immediately operate their workspace (create the organisation, manage third-parties,
 * products, sales, treasury, accounting, settings, etc.).
 *
 * <p>Idempotent: re-running for the same tenant/user reuses the existing role and
 * assignment instead of creating duplicates.</p>
 */
@Component
@Primary
public class RolesTenantOwnerRoleProvisioner implements TenantOwnerRoleProvisioner {

    static final String OWNER_ROLE_CODE = "OWNER";
    private static final String OWNER_ROLE_NAME = "Propriétaire";

    /**
     * Full business permission set for a tenant owner. {@code tenant:admin} unlocks the
     * governance/administration policies (which accept it explicitly); the concrete
     * {@code *:write} permissions satisfy the controllers that check them literally.
     */
    private static final Set<String> OWNER_PERMISSIONS = Set.of(
            "tenant:admin",
            "organizations:write",
            "third-parties:write",
            "products:write",
            "sales:write",
            "resources:write",
            "treasury:manage",
            "treasury:read",
            "treasury:settle-invoices",
            "accounting:read",
            "accounting:write",
            "accounting:post",
            "settings:read",
            "settings:write");

    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository assignmentRepository;

    public RolesTenantOwnerRoleProvisioner(RoleRepository roleRepository,
            UserRoleAssignmentRepository assignmentRepository) {
        this.roleRepository = roleRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public Mono<Void> provisionOwner(UUID tenantId, UUID userId) {
        if (tenantId == null || userId == null) {
            return Mono.empty();
        }
        return ensureRole(tenantId)
                .flatMap(role -> ensureAssignment(tenantId, userId, role))
                .then();
    }

    private Mono<Role> ensureRole(UUID tenantId) {
        return roleRepository.findByTenantId(tenantId)
                .filter(role -> OWNER_ROLE_CODE.equalsIgnoreCase(role.code()))
                .next()
                .switchIfEmpty(roleRepository.save(Role.create(
                        tenantId,
                        OWNER_ROLE_CODE,
                        OWNER_ROLE_NAME,
                        RoleScopeType.TENANT,
                        new LinkedHashSet<>(OWNER_PERMISSIONS))));
    }

    private Mono<Void> ensureAssignment(UUID tenantId, UUID userId, Role role) {
        return assignmentRepository.findByTenantIdAndUserId(tenantId, userId)
                .filter(assignment -> assignment.roleId().equals(role.id())
                        && assignment.scopeType() == RoleScopeType.TENANT)
                .next()
                .switchIfEmpty(assignmentRepository.save(UserRoleAssignment.assign(
                        tenantId,
                        userId,
                        role.id(),
                        RoleScopeType.TENANT,
                        null)))
                .then();
    }
}
