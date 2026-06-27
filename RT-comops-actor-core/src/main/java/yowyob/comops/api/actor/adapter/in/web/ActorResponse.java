package yowyob.comops.api.actor.adapter.in.web;

import yowyob.comops.api.actor.domain.model.Actor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ActorResponse(
        UUID id,
        UUID tenantId,
        UUID organizationId,
        String firstName,
        String lastName,
        String name,
        String displayName,
        String phoneNumber,
        String email,
        String description,
        String type,
        String gender,
        String photoUri,
        UUID photoId,
        String nationality,
        LocalDate birthDate,
        String profession,
        String biography,
        Set<UUID> addresses,
        Set<UUID> contacts,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    public static ActorResponse from(Actor actor) {
        return new ActorResponse(
                actor.id(),
                actor.tenantId(),
                actor.organizationId(),
                actor.firstName(),
                actor.lastName(),
                actor.name(),
                actor.displayName(),
                actor.phoneNumber(),
                actor.email(),
                actor.description(),
                actor.type(),
                actor.gender(),
                actor.photoUri(),
                actor.photoId(),
                actor.nationality(),
                actor.birthDate(),
                actor.profession(),
                actor.biography(),
                actor.addresses(),
                actor.contacts(),
                actor.createdAt(),
                actor.updatedAt(),
                actor.deletedAt());
    }
}
