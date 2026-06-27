package yowyob.comops.api.kernel.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.ReactiveSubscription.Message;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtDenyListService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtDenyListService.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final JwtDenyListProperties properties;

    /** jti -> expiry epoch millis (revoked entries; cleaned up lazily and via scheduled task). */
    private final ConcurrentHashMap<String, Long> localDenyList = new ConcurrentHashMap<>();

    public JwtDenyListService(ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
            JwtDenyListProperties properties) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    void initialize() {
        if (!properties.isEnabled() || redisTemplate == null) {
            return;
        }
        ReactiveSetOperations<String, String> setOperations = redisTemplate.opsForSet();
        if (setOperations == null) {
            LOG.warn("JWT denylist Redis set operations are unavailable; using local in-memory denylist only.");
            return;
        }
        // Load persisted revoked jtis on startup (best-effort)
        setOperations.members(properties.getRedisSetKey())
                .doOnNext(entry -> parseEntry(entry).ifPresent(p -> localDenyList.put(p.jti(), p.expiryEpochMs())))
                .doOnError(e -> LOG.warn("Could not warm up JWT denylist from Redis", e))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
        // Subscribe to cross-instance revocation events
        redisTemplate.listenTo(ChannelTopic.of(properties.getRedisChannel()))
                .map(Message::getMessage)
                .doOnNext(payload -> parseEntry(payload).ifPresent(p -> localDenyList.put(p.jti(), p.expiryEpochMs())))
                .doOnError(e -> LOG.warn("JWT denylist subscription error; will not auto-recover until restart", e))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    /** Hot-path: returns true if the jti is currently revoked. O(1), no I/O. */
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank() || !properties.isEnabled()) {
            return false;
        }
        Long expiry = localDenyList.get(jti);
        if (expiry == null) {
            return false;
        }
        if (expiry <= System.currentTimeMillis()) {
            localDenyList.remove(jti, expiry);
            return false;
        }
        return true;
    }

    /** Revoke a token by jti until its expiry. Persists to Redis and broadcasts to peers. */
    public Mono<Void> revoke(String jti, Instant tokenExpiresAt) {
        if (jti == null || jti.isBlank() || tokenExpiresAt == null) {
            return Mono.empty();
        }
        Duration remaining = Duration.between(Instant.now(), tokenExpiresAt);
        if (remaining.isNegative() || remaining.isZero()) {
            return Mono.empty();
        }
        long expiryMs = tokenExpiresAt.toEpochMilli();
        localDenyList.put(jti, expiryMs);
        if (!properties.isEnabled() || redisTemplate == null) {
            return Mono.empty();
        }
        String entry = jti + ":" + expiryMs;
        return redisTemplate.opsForSet().add(properties.getRedisSetKey(), entry)
                .then(redisTemplate.expire(properties.getRedisSetKey(),
                        Duration.ofDays(7))) // umbrella TTL on the set itself
                .then(redisTemplate.convertAndSend(properties.getRedisChannel(), entry))
                .doOnError(e -> LOG.warn("Failed to persist JWT denylist entry for jti={}", jti, e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    @Scheduled(fixedDelay = 60_000L)
    void evictExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = localDenyList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() <= now) {
                it.remove();
            }
        }
    }

    private java.util.Optional<DenyListEntry> parseEntry(String raw) {
        if (raw == null) {
            return java.util.Optional.empty();
        }
        int sep = raw.lastIndexOf(':');
        if (sep < 0) {
            return java.util.Optional.empty();
        }
        try {
            String jti = raw.substring(0, sep);
            long expiry = Long.parseLong(raw.substring(sep + 1));
            return java.util.Optional.of(new DenyListEntry(jti, expiry));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }

    private record DenyListEntry(String jti, long expiryEpochMs) {
    }
}
