package yowyob.comops.api.roles.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRoleAssignmentSpringDataRepository
        extends ReactiveCrudRepository<UserRoleAssignmentEntity, UUID> {

    Flux<UserRoleAssignmentEntity> findAllByTenantIdAndUserId(UUID tenantId, UUID userId);

    Mono<UserRoleAssignmentEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Flux<UserRoleAssignmentEntity> findAllByTenantIdAndRoleId(UUID tenantId, UUID roleId);

    Mono<Void> deleteByIdAndTenantId(UUID id, UUID tenantId);
}
