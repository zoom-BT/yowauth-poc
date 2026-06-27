package yowyob.comops.api.actor.application.service;

import yowyob.comops.api.actor.application.port.in.CreateActorCommand;
import yowyob.comops.api.actor.application.port.in.CreateActorUseCase;
import yowyob.comops.api.actor.application.port.out.ActorRepository;
import yowyob.comops.api.actor.domain.DuplicateActorEmailException;
import yowyob.comops.api.actor.domain.model.Actor;
import yowyob.comops.api.file.application.port.out.StoredFileRepository;
import java.util.UUID;
import java.util.Objects;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ActorApplicationService implements CreateActorUseCase {

    private final ActorRepository actorRepository;
    private final StoredFileRepository storedFileRepository;

    public ActorApplicationService(ActorRepository actorRepository, StoredFileRepository storedFileRepository) {
        this.actorRepository = actorRepository;
        this.storedFileRepository = storedFileRepository;
    }

    @Override
    public Mono<Actor> createActor(CreateActorCommand command) {
        Objects.requireNonNull(command, "command is required");

        Actor actor = Actor.create(
                command.tenantId(),
                command.organizationId(),
                command.firstName(),
                command.lastName(),
                command.name(),
                command.phoneNumber(),
                command.email(),
                command.description(),
                command.type(),
                command.gender(),
                command.photoUri(),
                command.photoId(),
                command.nationality(),
                command.birthDate(),
                command.profession(),
                command.biography(),
                command.addresses(),
                command.contacts());

        Mono<Void> validatePhoto = ensureStoredFileExists(command.tenantId(), command.photoId(), "photoId");

        if (actor.email() == null) {
            return validatePhoto.then(actorRepository.save(actor));
        }

        return validatePhoto.then(actorRepository.existsActiveByEmail(actor.tenantId(), actor.email())
                .flatMap(exists -> exists
                        ? Mono.error(new DuplicateActorEmailException(actor.email()))
                        : actorRepository.save(actor)));
    }

    private Mono<Void> ensureStoredFileExists(UUID tenantId, UUID fileId, String fieldName) {
        if (fileId == null) {
            return Mono.empty();
        }
        return storedFileRepository.findById(tenantId, fileId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(fieldName + " does not reference an existing file")))
                .then();
    }
}
