package yowyob.comops.api.common.adapter.in.web;

import yowyob.comops.api.common.domain.model.AddressType;
import yowyob.comops.api.common.domain.model.AddressableType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateAddressRequest(
        @NotNull AddressableType addressableType,
        @NotNull UUID addressableId,
        @NotNull AddressType type,
        @NotBlank String addressLine1,
        String addressLine2,
        String city,
        String state,
        String locality,
        UUID countryId,
        String zipCode,
        String postalCode,
        String poBox,
        boolean isDefault,
        String neighborhood,
        String informalDescription,
        Double latitude,
        Double longitude) {
}
