package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "kernel", name = "client_application")
public record ClientApplicationEntity(
        @Id UUID id,
        Instant createdAt,
        Instant updatedAt,
        String clientId,
        String name,
        String description,
        String secretHash,
        String status,
        boolean systemManaged,
        String[] allowedServiceCodes,
        Instant lastAuthenticatedAt,
        Instant secretRotatedAt) implements PersistableEntity {
}
