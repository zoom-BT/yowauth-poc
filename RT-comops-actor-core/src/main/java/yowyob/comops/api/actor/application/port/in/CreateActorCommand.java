package yowyob.comops.api.actor.application.port.in;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CreateActorCommand(
        UUID tenantId,
        UUID organizationId,
        String firstName,
        String lastName,
        String name,
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
        Set<UUID> contacts) {

    public CreateActorCommand(
            UUID tenantId,
            String firstName,
            String lastName,
            String phoneNumber,
            String email,
            String gender,
            String nationality,
            LocalDate birthDate,
            String profession,
            String biography) {
        this(
                tenantId,
                null,
                firstName,
                lastName,
                null,
                phoneNumber,
                email,
                null,
                null,
                gender,
                null,
                null,
                nationality,
                birthDate,
                profession,
                biography,
                Set.of(),
                Set.of());
    }
}
