package yowyob.comops.api.common.application.port.in;

import yowyob.comops.api.common.domain.model.ContactableType;
import java.util.UUID;

public record CreateContactCommand(
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
        String secondaryEmail) {
}
