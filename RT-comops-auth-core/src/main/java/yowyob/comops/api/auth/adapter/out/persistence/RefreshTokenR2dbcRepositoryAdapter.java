package yowyob.comops.api.auth.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.application.port.out.RefreshTokenRepository;
import yowyob.comops.api.auth.domain.model.RefreshToken;

@Component
@Profile("r2dbc")
public class RefreshTokenR2dbcRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenSpringDataRepository repository;

    public RefreshTokenR2dbcRepositoryAdapter(RefreshTokenSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<RefreshToken> save(RefreshToken token) {
        return repository.save(toEntity(token)).map(this::toDomain);
    }

    @Override
    public Mono<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public Mono<Long> revokeAllForUser(UUID tenantId, UUID userId, Instant at) {
        return repository.revokeAllForUser(tenantId, userId, at);
    }

    private RefreshTokenEntity toEntity(RefreshToken token) {
        return new RefreshTokenEntity(token.id(), token.tenantId(), token.userId(), token.tokenHash(),
                token.issuedAt(), token.expiresAt(), token.revokedAt(), token.replacedById(),
                token.remoteIp(), token.userAgent(), token.createdAt(), token.updatedAt());
    }

    private RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(e.id(), e.tenantId(), e.userId(), e.tokenHash(), e.issuedAt(), e.expiresAt(),
                e.revokedAt(), e.replacedById(), e.remoteIp(), e.userAgent(), e.createdAt(), e.updatedAt());
    }
}
