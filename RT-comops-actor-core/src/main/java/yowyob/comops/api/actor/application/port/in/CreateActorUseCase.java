package yowyob.comops.api.actor.application.port.in;

import yowyob.comops.api.actor.domain.model.Actor;
import reactor.core.publisher.Mono;

public interface CreateActorUseCase {

    Mono<Actor> createActor(CreateActorCommand command);
}
