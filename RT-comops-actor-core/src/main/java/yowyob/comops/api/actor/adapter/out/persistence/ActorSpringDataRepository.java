package yowyob.comops.api.actor.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ActorSpringDataRepository extends ReactiveCrudRepository<ActorEntity, UUID> {

    Mono<Boolean> existsByTenantIdAndDeletedAtIsNullAndEmailIgnoreCase(UUID tenantId, String email);

    Mono<ActorEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
