package yowyob.comops.api.actor.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BusinessActorProfileSpringDataRepository
        extends ReactiveCrudRepository<BusinessActorProfileEntity, UUID> {

    Mono<BusinessActorProfileEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Mono<BusinessActorProfileEntity> findByTenantIdAndActorId(UUID tenantId, UUID actorId);

    Flux<BusinessActorProfileEntity> findAllByTenantId(UUID tenantId);
}
