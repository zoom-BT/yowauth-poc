package yowyob.comops.api.actor.application.port.out;

import yowyob.comops.api.actor.domain.model.Actor;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface ActorRepository {

    Mono<Boolean> existsActiveByEmail(UUID tenantId, String email);

    Mono<Actor> findById(UUID tenantId, UUID actorId);

    Mono<Actor> save(Actor actor);
}
