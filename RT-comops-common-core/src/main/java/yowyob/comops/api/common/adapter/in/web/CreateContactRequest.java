package yowyob.comops.api.common.adapter.in.web;

import yowyob.comops.api.common.domain.model.ContactableType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateContactRequest(
        @NotNull ContactableType contactableType,
        @NotNull UUID contactableId,
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
