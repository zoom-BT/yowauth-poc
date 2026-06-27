package yowyob.comops.api.common.application.port.in;

import yowyob.comops.api.common.domain.model.AddressType;
import yowyob.comops.api.common.domain.model.AddressableType;
import java.util.UUID;

public record CreateAddressCommand(
        UUID tenantId,
        AddressableType addressableType,
        UUID addressableId,
        AddressType type,
        String addressLine1,
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
