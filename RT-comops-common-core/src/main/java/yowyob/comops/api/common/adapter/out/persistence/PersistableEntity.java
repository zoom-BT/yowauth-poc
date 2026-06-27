package yowyob.comops.api.common.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

public interface PersistableEntity extends Persistable<UUID> {

    UUID id();

    Instant createdAt();

    Instant updatedAt();

    @Override
    default UUID getId() {
        return id();
    }

    @Override
    default boolean isNew() {
        return createdAt().equals(updatedAt());
    }
}
