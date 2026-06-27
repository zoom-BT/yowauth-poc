package yowyob.comops.api.actor.adapter.out.persistence;

import yowyob.comops.api.actor.application.port.out.ActorRepository;
import yowyob.comops.api.actor.domain.model.Actor;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryActorRepository implements ActorRepository {

    private final Map<UUID, Actor> actors = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> existsActiveByEmail(UUID tenantId, String email) {
        return Mono.fromSupplier(() -> actors.values().stream()
                .filter(actor -> actor.tenantId().equals(tenantId))
                .filter(actor -> actor.deletedAt() == null)
                .anyMatch(actor -> email.equals(actor.email())));
    }

    @Override
    public Mono<Actor> findById(UUID tenantId, UUID actorId) {
        return Mono.justOrEmpty(actors.get(actorId))
                .filter(actor -> actor.tenantId().equals(tenantId))
                .filter(actor -> actor.deletedAt() == null);
    }

    @Override
    public Mono<Actor> save(Actor actor) {
        return Mono.fromSupplier(() -> {
            actors.put(actor.id(), actor);
            return actor;
        });
    }
}
