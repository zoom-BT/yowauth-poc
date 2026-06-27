package yowyob.comops.api.auth.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.application.port.out.RefreshTokenRepository;
import yowyob.comops.api.auth.config.RefreshTokenProperties;
import yowyob.comops.api.auth.domain.model.RefreshToken;

@Service
public class RefreshTokenService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int TOKEN_BYTES = 48;

    private final RefreshTokenRepository repository;
    private final RefreshTokenProperties properties;

    public RefreshTokenService(RefreshTokenRepository repository, RefreshTokenProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public Mono<IssuedToken> issue(UUID tenantId, UUID userId, String remoteIp, String userAgent) {
        if (!properties.isEnabled()) {
            return Mono.empty();
        }
        String rawToken = generateRawToken();
        String hash = sha256(rawToken);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getTtl());
        RefreshToken token = new RefreshToken(UUID.randomUUID(), tenantId, userId, hash, now, expiresAt,
                null, null, remoteIp, userAgent, now, now);
        return repository.save(token)
                .map(saved -> new IssuedToken(saved, rawToken, properties.getTtl().toSeconds()));
    }

    /** Validate and rotate: revokes the presented token and issues a new pair. */
    public Mono<IssuedToken> rotate(String presentedRawToken, String remoteIp, String userAgent) {
        if (!properties.isEnabled() || presentedRawToken == null || presentedRawToken.isBlank()) {
            return Mono.error(new InvalidRefreshTokenException("Refresh token is required."));
        }
        String hash = sha256(presentedRawToken);
        return repository.findByTokenHash(hash)
                .switchIfEmpty(Mono.error(new InvalidRefreshTokenException("Refresh token not found.")))
                .flatMap(existing -> {
                    Instant now = Instant.now();
                    if (!existing.isUsable(now)) {
                        return Mono.error(new InvalidRefreshTokenException("Refresh token expired or revoked."));
                    }
                    String newRaw = generateRawToken();
                    String newHash = sha256(newRaw);
                    Instant expiresAt = now.plus(properties.getTtl());
                    UUID newId = UUID.randomUUID();
                    RefreshToken newToken = new RefreshToken(newId, existing.tenantId(), existing.userId(),
                            newHash, now, expiresAt, null, null, remoteIp, userAgent, now, now);
                    RefreshToken revoked = existing.revoke(now, newId);
                    return repository.save(revoked)
                            .then(repository.save(newToken))
                            .map(saved -> new IssuedToken(saved, newRaw, properties.getTtl().toSeconds()));
                });
    }

    public Mono<Long> revokeAllForUser(UUID tenantId, UUID userId) {
        return repository.revokeAllForUser(tenantId, userId, Instant.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RNG.nextBytes(bytes);
        return properties.getPrefix() + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record IssuedToken(RefreshToken token, String rawValue, long ttlSeconds) {
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) {
            super(message);
        }
    }
}
