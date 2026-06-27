package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "kernel", name = "platform_external_service")
public record ExternalServiceEntity(
        @Id UUID id,
        Instant createdAt,
        Instant updatedAt,
        String code,
        String displayName,
        String description,
        boolean active) implements PersistableEntity {
}
