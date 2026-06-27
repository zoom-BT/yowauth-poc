package yowyob.comops.api.actor.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CreateActorRequest(
        UUID organizationId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @JsonAlias("displayName") String name,
        String phoneNumber,
        @Email String email,
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
}
