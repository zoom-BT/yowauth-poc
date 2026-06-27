package yowyob.comops.api.auth.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RefreshTokenSpringDataRepository extends ReactiveCrudRepository<RefreshTokenEntity, UUID> {

    Mono<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE auth_core.refresh_token
               SET revoked_at = :at,
                   updated_at = :at
             WHERE tenant_id = :tenantId
               AND user_id = :userId
               AND revoked_at IS NULL
            """)
    Mono<Long> revokeAllForUser(UUID tenantId, UUID userId, Instant at);
}
