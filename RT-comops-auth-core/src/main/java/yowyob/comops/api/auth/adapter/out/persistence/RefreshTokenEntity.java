package yowyob.comops.api.auth.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "auth_core", name = "refresh_token")
public record RefreshTokenEntity(
        @Id UUID id,
        UUID tenantId,
        UUID userId,
        String tokenHash,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt,
        UUID replacedById,
        String remoteIp,
        String userAgent,
        Instant createdAt,
        Instant updatedAt) implements PersistableEntity {
}
