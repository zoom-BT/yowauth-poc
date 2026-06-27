package yowyob.comops.api.kernel.config;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserSessionTokenClaims(
        UUID tenantId,
        UUID organizationId,
        UUID agencyId,
        UUID userId,
        UUID actorId,
        Set<String> permissions,
        Instant issuedAt,
        Instant expiresAt,
        String tokenId) {

    public static UserSessionTokenClaims legacy(UUID tenantId, UUID userId, UUID actorId, Instant issuedAt,
            Instant expiresAt) {
        return new UserSessionTokenClaims(tenantId, null, null, userId, actorId, Set.of(), issuedAt, expiresAt, null);
    }

    public static UserSessionTokenClaims fromJwtClaims(JwtClaims claims) {
        return new UserSessionTokenClaims(claims.tenantId(), claims.organizationId(), claims.agencyId(),
                claims.userId(), claims.actorId(), claims.permissions(), claims.issuedAt(), claims.expiresAt(),
                claims.jwtId());
    }
}
