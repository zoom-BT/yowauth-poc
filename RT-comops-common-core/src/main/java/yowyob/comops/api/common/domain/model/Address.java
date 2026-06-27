package yowyob.comops.api.common.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Address(
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

    public Address {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(addressableType, "addressableType is required");
        Objects.requireNonNull(addressableId, "addressableId is required");
        Objects.requireNonNull(type, "type is required");
        if (addressLine1 == null || addressLine1.isBlank()) {
            throw new IllegalArgumentException("addressLine1 is required");
        }
        validateLatitude(latitude);
        validateLongitude(longitude);
    }

    public AddressType addressType() {
        return type;
    }

    private static void validateLatitude(Double latitude) {
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
    }

    private static void validateLongitude(Double longitude) {
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }
}
