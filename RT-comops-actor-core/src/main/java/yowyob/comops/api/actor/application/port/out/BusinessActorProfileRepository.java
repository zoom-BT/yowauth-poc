package yowyob.comops.api.actor.application.port.out;

import yowyob.comops.api.actor.domain.model.BusinessActorProfile;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BusinessActorProfileRepository {

    Mono<BusinessActorProfile> findById(UUID tenantId, UUID businessActorProfileId);

    Mono<BusinessActorProfile> findByActorId(UUID tenantId, UUID actorId);

    Flux<BusinessActorProfile> findByTenantId(UUID tenantId);

    Mono<BusinessActorProfile> save(BusinessActorProfile profile);
}
