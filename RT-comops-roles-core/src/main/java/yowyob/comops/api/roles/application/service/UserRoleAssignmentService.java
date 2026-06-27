package yowyob.comops.api.roles.application.service;

import yowyob.comops.api.kernel.application.port.out.BusinessEventPublisher;
import yowyob.comops.api.kernel.application.port.out.ReactivePermissionCache;
import yowyob.comops.api.kernel.domain.model.BusinessEvent;
import yowyob.comops.api.roles.application.port.in.AssignRoleToUserCommand;
import yowyob.comops.api.roles.application.port.in.AssignRoleToUserUseCase;
import yowyob.comops.api.roles.application.port.in.ListUserRoleAssignmentsUseCase;
import yowyob.comops.api.roles.application.port.in.RevokeRoleAssignmentUseCase;
import yowyob.comops.api.roles.application.port.out.UserRoleAssignmentRepository;
import yowyob.comops.api.roles.domain.model.RoleScopeType;
import yowyob.comops.api.roles.domain.model.UserRoleAssignment;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserRoleAssignmentService implements AssignRoleToUserUseCase, ListUserRoleAssignmentsUseCase, RevokeRoleAssignmentUseCase {

    private final UserRoleAssignmentRepository repository;
    private final Optional<ReactivePermissionCache> permissionCache;
    private final BusinessEventPublisher businessEventPublisher;

    public UserRoleAssignmentService(UserRoleAssignmentRepository repository,
            Optional<ReactivePermissionCache> permissionCache,
            BusinessEventPublisher businessEventPublisher) {
        this.repository = repository;
        this.permissionCache = permissionCache;
        this.businessEventPublisher = businessEventPublisher;
    }

    @Override
    public Mono<UserRoleAssignment> assign(AssignRoleToUserCommand command) {
        Objects.requireNonNull(command, "command is required");
        UserRoleAssignment assignment = command.scopeType() != null || command.scopeId() != null
                ? UserRoleAssignment.assign(command.tenantId(), command.userId(), command.roleId(),
                        RoleScopeType.from(command.scopeType()), command.scopeId())
                : UserRoleAssignment.assign(command.tenantId(), command.userId(), command.roleId(), command.scope());
        return repository.save(assignment)
                .flatMap(saved -> permissionCache.map(cache -> cache.evict(command.tenantId(), command.userId())
                        .thenReturn(saved)).orElseGet(() -> Mono.just(saved)))
                .flatMap(saved -> businessEventPublisher.publish(
                        BusinessEvent.now(saved.tenantId(), null, "ROLE_ASSIGNED", "USER_ROLE_ASSIGNMENT", saved.id(),
                                Map.of("userId", saved.userId().toString(), "roleId", saved.roleId().toString())))
                        .onErrorResume(ex -> Mono.empty())
                        .thenReturn(saved));
    }

    @Override
    public Flux<UserRoleAssignment> listAssignments(UUID tenantId, UUID userId) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(userId, "userId is required");
        return repository.findByTenantIdAndUserId(tenantId, userId);
    }

    @Override
    public Mono<Void> revoke(UUID tenantId, UUID assignmentId) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(assignmentId, "assignmentId is required");
        return repository.findById(tenantId, assignmentId)
                .flatMap(assignment -> repository.deleteById(tenantId, assignmentId)
                        .then(permissionCache.map(cache -> cache.evict(tenantId, assignment.userId()))
                                .orElseGet(Mono::empty))
                        .then(businessEventPublisher.publish(
                                BusinessEvent.now(tenantId, null, "ROLE_REVOKED", "USER_ROLE_ASSIGNMENT", assignmentId,
                                        Map.of("userId", assignment.userId().toString(),
                                                "roleId", assignment.roleId().toString())))
                                .onErrorResume(ex -> Mono.empty())));
    }
}
