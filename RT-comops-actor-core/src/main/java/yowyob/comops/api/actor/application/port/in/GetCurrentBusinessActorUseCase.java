package yowyob.comops.api.actor.application.port.in;

import yowyob.comops.api.actor.domain.model.BusinessActorProfile;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface GetCurrentBusinessActorUseCase {

    Mono<BusinessActorProfile> getByUser(UUID tenantId, UUID userId);
}
