package yowyob.comops.api.actor.adapter.out.persistence;

import yowyob.comops.api.actor.application.port.out.ActorRepository;
import yowyob.comops.api.actor.domain.model.Actor;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class ActorR2dbcRepositoryAdapter implements ActorRepository {

    private final ActorSpringDataRepository repository;

    public ActorR2dbcRepositoryAdapter(ActorSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Boolean> existsActiveByEmail(UUID tenantId, String email) {
        return repository.existsByTenantIdAndDeletedAtIsNullAndEmailIgnoreCase(tenantId, email);
    }

    @Override
    public Mono<Actor> findById(UUID tenantId, UUID actorId) {
        return repository.findByIdAndTenantIdAndDeletedAtIsNull(actorId, tenantId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Actor> save(Actor actor) {
        return repository.save(toEntity(actor)).map(this::toDomain);
    }

    private ActorEntity toEntity(Actor actor) {
        return new ActorEntity(actor.id(), actor.tenantId(), actor.createdAt(), actor.updatedAt(),
                actor.organizationId(), actor.firstName(), actor.lastName(), actor.name(), actor.phoneNumber(),
                actor.email(), actor.description(), actor.type(), actor.gender(), actor.photoUri(), actor.photoId(),
                actor.nationality(), actor.birthDate(), actor.profession(), actor.biography(), actor.addresses(),
                actor.contacts(), actor.deletedAt());
    }

    private Actor toDomain(ActorEntity entity) {
        return Actor.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(), entity.updatedAt(),
                entity.organizationId(), entity.firstName(), entity.lastName(), entity.name(), entity.phoneNumber(),
                entity.email(), entity.description(), entity.type(), entity.gender(), entity.photoUri(),
                entity.photoId(), entity.nationality(), entity.birthDate(), entity.profession(), entity.biography(),
                entity.addresses(), entity.contacts(), entity.deletedAt());
    }
}
