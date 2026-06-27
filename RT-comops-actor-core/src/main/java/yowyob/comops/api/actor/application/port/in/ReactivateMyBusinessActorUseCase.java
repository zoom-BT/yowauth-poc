package yowyob.comops.api.actor.application.port.in;

import yowyob.comops.api.actor.domain.model.BusinessActorProfile;
import reactor.core.publisher.Mono;

public interface ReactivateMyBusinessActorUseCase {

    Mono<BusinessActorProfile> reactivate(ReactivateMyBusinessActorCommand command);
}
