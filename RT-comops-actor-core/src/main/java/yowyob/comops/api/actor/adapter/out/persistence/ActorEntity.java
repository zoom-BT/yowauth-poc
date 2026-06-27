package yowyob.comops.api.actor.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "actor", name = "actor")
public record ActorEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
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
        Set<UUID> contacts,
        Instant deletedAt) implements PersistableEntity {
}
