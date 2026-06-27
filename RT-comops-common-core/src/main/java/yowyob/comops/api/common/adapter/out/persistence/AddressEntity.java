package yowyob.comops.api.common.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "common_core", name = "address")
public record AddressEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        String addressableType,
        UUID addressableId,
        String type,
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
        Double longitude) implements PersistableEntity {
}
