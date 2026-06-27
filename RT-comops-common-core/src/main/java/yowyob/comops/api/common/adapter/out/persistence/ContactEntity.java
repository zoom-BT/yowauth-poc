package yowyob.comops.api.common.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "common_core", name = "contact")
public record ContactEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        String contactableType,
        UUID contactableId,
        String firstName,
        String lastName,
        String title,
        boolean isEmailVerified,
        boolean isPhoneNumberVerified,
        boolean isFavorite,
        String phoneNumber,
        String secondaryPhoneNumber,
        String faxNumber,
        String email,
        String secondaryEmail,
        Instant emailVerifiedAt,
        Instant phoneVerifiedAt) implements PersistableEntity {
}
