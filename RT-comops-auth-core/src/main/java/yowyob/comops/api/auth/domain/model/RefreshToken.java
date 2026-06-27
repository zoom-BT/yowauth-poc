package yowyob.comops.api.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        UUID id,
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
        Instant updatedAt) {

    public boolean isUsable(Instant asOf) {
        return revokedAt == null && expiresAt.isAfter(asOf);
    }

    public RefreshToken revoke(Instant at, UUID replacedById) {
        return new RefreshToken(id, tenantId, userId, tokenHash, issuedAt, expiresAt, at, replacedById,
                remoteIp, userAgent, createdAt, at);
    }
}
