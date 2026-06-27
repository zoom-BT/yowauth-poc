package yowyob.comops.api.common.adapter.in.web;

import yowyob.comops.api.common.domain.model.Address;
import yowyob.comops.api.common.domain.model.AddressType;
import yowyob.comops.api.common.domain.model.AddressableType;
import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
        UUID id,
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
        Double longitude,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(address.id(), address.tenantId(), address.addressableType(), address.addressableId(),
                address.type(), address.addressLine1(), address.addressLine2(), address.city(), address.state(),
                address.locality(), address.countryId(), address.zipCode(), address.postalCode(), address.poBox(),
                address.isDefault(), address.neighborhood(), address.informalDescription(), address.latitude(),
                address.longitude(), address.createdAt(), address.updatedAt(), address.deletedAt());
    }
}
