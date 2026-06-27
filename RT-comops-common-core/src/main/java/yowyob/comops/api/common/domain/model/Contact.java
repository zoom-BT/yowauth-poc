package yowyob.comops.api.common.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Contact(
        UUID id,
        UUID tenantId,
        ContactableType contactableType,
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
        Instant phoneVerifiedAt,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    public Contact {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(contactableType, "contactableType is required");
        Objects.requireNonNull(contactableId, "contactableId is required");
    }
}
