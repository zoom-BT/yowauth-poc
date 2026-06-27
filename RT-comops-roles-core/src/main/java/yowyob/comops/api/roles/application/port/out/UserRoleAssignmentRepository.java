package yowyob.comops.api.roles.application.port.out;

import yowyob.comops.api.roles.domain.model.UserRoleAssignment;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRoleAssignmentRepository {
    Mono<UserRoleAssignment> save(UserRoleAssignment assignment);

    Flux<UserRoleAssignment> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    Mono<UserRoleAssignment> findById(UUID tenantId, UUID assignmentId);

    Flux<UserRoleAssignment> findByTenantIdAndRoleId(UUID tenantId, UUID roleId);

    Mono<Void> deleteById(UUID tenantId, UUID assignmentId);
}
