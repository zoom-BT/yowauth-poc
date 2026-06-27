package yowyob.comops.api.common.adapter.in.web;

import yowyob.comops.api.common.domain.model.Contact;
import yowyob.comops.api.common.domain.model.ContactableType;
import java.time.Instant;
import java.util.UUID;

public record ContactResponse(
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

    public static ContactResponse from(Contact contact) {
        return new ContactResponse(contact.id(), contact.tenantId(), contact.contactableType(), contact.contactableId(),
                contact.firstName(), contact.lastName(), contact.title(), contact.isEmailVerified(),
                contact.isPhoneNumberVerified(), contact.isFavorite(), contact.phoneNumber(),
                contact.secondaryPhoneNumber(), contact.faxNumber(), contact.email(), contact.secondaryEmail(),
                contact.emailVerifiedAt(), contact.phoneVerifiedAt(), contact.createdAt(), contact.updatedAt(),
                contact.deletedAt());
    }
}
