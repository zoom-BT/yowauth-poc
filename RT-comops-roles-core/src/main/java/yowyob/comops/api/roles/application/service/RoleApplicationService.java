package yowyob.comops.api.roles.application.service;

import yowyob.comops.api.kernel.application.port.out.BusinessEventPublisher;
import yowyob.comops.api.kernel.domain.model.BusinessEvent;
import yowyob.comops.api.roles.application.port.in.CreateRoleCommand;
import yowyob.comops.api.roles.application.port.in.CreateRoleUseCase;
import yowyob.comops.api.roles.application.port.in.DeleteRoleUseCase;
import yowyob.comops.api.roles.application.port.in.GetRoleUseCase;
import yowyob.comops.api.roles.application.port.in.ListRolesUseCase;
import yowyob.comops.api.roles.application.port.out.RoleRepository;
import yowyob.comops.api.roles.domain.DuplicateRoleCodeException;
import yowyob.comops.api.roles.domain.model.Role;
import yowyob.comops.api.roles.domain.model.RoleScopeType;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RoleApplicationService implements CreateRoleUseCase, ListRolesUseCase, GetRoleUseCase, DeleteRoleUseCase {

    private final RoleRepository roleRepository;
    private final BusinessEventPublisher businessEventPublisher;

    public RoleApplicationService(RoleRepository roleRepository, BusinessEventPublisher businessEventPublisher) {
        this.roleRepository = roleRepository;
        this.businessEventPublisher = businessEventPublisher;
    }

    @Override
    public Mono<Role> createRole(CreateRoleCommand command) {
        Objects.requireNonNull(command, "command is required");
        Role role = Role.create(command.tenantId(), command.code(), command.name(),
                RoleScopeType.from(command.scopeType()), command.permissions());
        return roleRepository.existsByCode(role.tenantId(), role.code())
                .flatMap(exists -> exists
                        ? Mono.error(new DuplicateRoleCodeException(role.code()))
                        : roleRepository.save(role))
                .flatMap(saved -> businessEventPublisher.publish(
                        BusinessEvent.now(saved.tenantId(), null, "ROLE_CREATED", "ROLE", saved.id(),
                                Map.of("code", saved.code(), "name", saved.name())))
                        .onErrorResume(ex -> Mono.empty())
                        .thenReturn(saved));
    }

    @Override
    public Flux<Role> listRoles(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        return roleRepository.findByTenantId(tenantId);
    }

    @Override
    public Mono<Role> getRole(UUID tenantId, UUID roleId) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(roleId, "roleId is required");
        return roleRepository.findById(tenantId, roleId);
    }

    @Override
    public Mono<Void> deleteRole(UUID tenantId, UUID roleId) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(roleId, "roleId is required");
        return roleRepository.deleteById(tenantId, roleId)
                .then(businessEventPublisher.publish(
                        BusinessEvent.now(tenantId, null, "ROLE_DELETED", "ROLE", roleId, Map.of()))
                        .onErrorResume(ex -> Mono.empty()));
    }
}
