package yowyob.comops.api.roles.adapter.out.persistence;

import yowyob.comops.api.roles.application.port.out.UserRoleAssignmentRepository;
import yowyob.comops.api.roles.domain.model.UserRoleAssignment;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryUserRoleAssignmentRepository implements UserRoleAssignmentRepository {

    private final Map<UUID, UserRoleAssignment> assignments = new ConcurrentHashMap<>();

    @Override
    public Mono<UserRoleAssignment> save(UserRoleAssignment assignment) {
        return Mono.fromSupplier(() -> {
            assignments.put(assignment.id(), assignment);
            return assignment;
        });
    }

    @Override
    public Flux<UserRoleAssignment> findByTenantIdAndUserId(UUID tenantId, UUID userId) {
        return Flux.fromStream(assignments.values().stream()
                .filter(assignment -> assignment.tenantId().equals(tenantId))
                .filter(assignment -> assignment.userId().equals(userId)));
    }

    @Override
    public Mono<UserRoleAssignment> findById(UUID tenantId, UUID assignmentId) {
        return Mono.justOrEmpty(assignments.get(assignmentId))
                .filter(assignment -> assignment.tenantId().equals(tenantId));
    }

    @Override
    public Flux<UserRoleAssignment> findByTenantIdAndRoleId(UUID tenantId, UUID roleId) {
        return Flux.fromStream(assignments.values().stream()
                .filter(assignment -> assignment.tenantId().equals(tenantId))
                .filter(assignment -> assignment.roleId().equals(roleId)));
    }

    @Override
    public Mono<Void> deleteById(UUID tenantId, UUID assignmentId) {
        return Mono.fromRunnable(() -> {
            UserRoleAssignment existing = assignments.get(assignmentId);
            if (existing != null && existing.tenantId().equals(tenantId)) {
                assignments.remove(assignmentId);
            }
        });
    }
}
