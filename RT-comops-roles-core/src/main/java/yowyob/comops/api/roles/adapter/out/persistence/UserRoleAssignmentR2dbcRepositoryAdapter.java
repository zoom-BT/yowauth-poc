package yowyob.comops.api.roles.adapter.out.persistence;

import yowyob.comops.api.roles.application.port.out.UserRoleAssignmentRepository;
import yowyob.comops.api.roles.domain.model.UserRoleAssignment;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class UserRoleAssignmentR2dbcRepositoryAdapter implements UserRoleAssignmentRepository {

    private final UserRoleAssignmentSpringDataRepository repository;

    public UserRoleAssignmentR2dbcRepositoryAdapter(UserRoleAssignmentSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<UserRoleAssignment> save(UserRoleAssignment assignment) {
        UserRoleAssignmentEntity entity = new UserRoleAssignmentEntity(assignment.id(), assignment.tenantId(),
                assignment.createdAt(), assignment.updatedAt(), assignment.userId(), assignment.roleId(),
                assignment.scopeType().name(), assignment.scopeId(),
                assignment.scope());
        return repository.save(entity).map(this::toDomain);
    }

    @Override
    public Flux<UserRoleAssignment> findByTenantIdAndUserId(UUID tenantId, UUID userId) {
        return repository.findAllByTenantIdAndUserId(tenantId, userId)
                .map(this::toDomain);
    }

    @Override
    public Mono<UserRoleAssignment> findById(UUID tenantId, UUID assignmentId) {
        return repository.findByIdAndTenantId(assignmentId, tenantId)
                .map(this::toDomain);
    }

    @Override
    public Flux<UserRoleAssignment> findByTenantIdAndRoleId(UUID tenantId, UUID roleId) {
        return repository.findAllByTenantIdAndRoleId(tenantId, roleId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(UUID tenantId, UUID assignmentId) {
        return repository.deleteByIdAndTenantId(assignmentId, tenantId);
    }

    private UserRoleAssignment toDomain(UserRoleAssignmentEntity entity) {
        return UserRoleAssignment.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(), entity.updatedAt(),
                entity.userId(), entity.roleId(), entity.scopeType(), entity.scopeId(), entity.scope());
    }
}
