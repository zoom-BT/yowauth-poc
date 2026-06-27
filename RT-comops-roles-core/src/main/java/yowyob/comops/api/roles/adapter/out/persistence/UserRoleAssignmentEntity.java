package yowyob.comops.api.roles.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "roles_core", name = "user_role_assignment")
public record UserRoleAssignmentEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        UUID userId,
        UUID roleId,
        String scopeType,
        UUID scopeId,
        String scope) implements PersistableEntity {
}
