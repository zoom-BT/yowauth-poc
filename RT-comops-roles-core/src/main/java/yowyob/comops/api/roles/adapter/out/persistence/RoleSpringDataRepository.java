package yowyob.comops.api.roles.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RoleSpringDataRepository extends ReactiveCrudRepository<RoleEntity, UUID> {

    Mono<Boolean> existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

    Mono<RoleEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    reactor.core.publisher.Flux<RoleEntity> findAllByTenantId(UUID tenantId);

    Mono<Void> deleteByIdAndTenantId(UUID id, UUID tenantId);
}
