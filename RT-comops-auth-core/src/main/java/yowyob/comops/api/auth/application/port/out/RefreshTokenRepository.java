package yowyob.comops.api.auth.application.port.out;

import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.domain.model.RefreshToken;

public interface RefreshTokenRepository {

    Mono<RefreshToken> save(RefreshToken token);

    Mono<RefreshToken> findByTokenHash(String tokenHash);

    Mono<Long> revokeAllForUser(UUID tenantId, UUID userId, Instant at);
}
