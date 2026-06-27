package yowyob.comops.api.auth.adapter.out.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.application.port.out.RefreshTokenRepository;
import yowyob.comops.api.auth.domain.model.RefreshToken;

@Component
@Profile("test-memory")
public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<UUID, RefreshToken> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byHash = new ConcurrentHashMap<>();

    @Override
    public Mono<RefreshToken> save(RefreshToken token) {
        return Mono.fromSupplier(() -> {
            byId.put(token.id(), token);
            byHash.put(token.tokenHash(), token.id());
            return token;
        });
    }

    @Override
    public Mono<RefreshToken> findByTokenHash(String tokenHash) {
        return Mono.fromSupplier(() -> {
            UUID id = byHash.get(tokenHash);
            return id == null ? null : byId.get(id);
        });
    }

    @Override
    public Mono<Long> revokeAllForUser(UUID tenantId, UUID userId, Instant at) {
        return Mono.fromSupplier(() -> byId.entrySet().stream()
                .filter(e -> e.getValue().tenantId().equals(tenantId)
                        && e.getValue().userId().equals(userId)
                        && e.getValue().revokedAt() == null)
                .peek(e -> byId.put(e.getKey(), e.getValue().revoke(at, null)))
                .count());
    }
}
