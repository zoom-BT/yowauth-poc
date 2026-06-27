package yowyob.comops.api.kernel.adapter.out.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.kernel.application.port.out.ReactivePermissionCache;
import yowyob.comops.api.kernel.config.RedisPermissionCacheProperties;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "iwm.redis.permission-cache", name = "enabled", havingValue = "true")
public class RedisReactivePermissionCache implements ReactivePermissionCache {

    private static final TypeReference<Set<String>> PERMISSIONS_TYPE = new TypeReference<>() { };

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisPermissionCacheProperties properties;

    public RedisReactivePermissionCache(ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper,
            RedisPermissionCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Mono<Set<String>> get(UUID tenantId, UUID userId) {
        return redisTemplate.opsForValue().get(key(tenantId, userId))
                .flatMap(this::deserialize)
                .onErrorResume(exception -> Mono.empty());
    }

    @Override
    public Mono<Void> put(UUID tenantId, UUID userId, Set<String> permissions) {
        return serialize(permissions)
                .flatMap(payload -> redisTemplate.opsForValue().set(key(tenantId, userId), payload, properties.getTtl()))
                .then();
    }

    @Override
    public Mono<Void> evict(UUID tenantId, UUID userId) {
        return redisTemplate.delete(key(tenantId, userId)).then();
    }

    private String key(UUID tenantId, UUID userId) {
        return properties.getKeyPrefix() + ":" + tenantId + ":" + userId;
    }

    private Mono<String> serialize(Set<String> permissions) {
        try {
            return Mono.just(objectMapper.writeValueAsString(Set.copyOf(new LinkedHashSet<>(permissions))));
        } catch (JsonProcessingException exception) {
            return Mono.error(new IllegalStateException("Unable to serialize permissions cache entry", exception));
        }
    }

    private Mono<Set<String>> deserialize(String payload) {
        try {
            return Mono.just(Set.copyOf(new LinkedHashSet<>(objectMapper.readValue(payload, PERMISSIONS_TYPE))));
        } catch (JsonProcessingException exception) {
            return Mono.error(new IllegalStateException("Unable to deserialize permissions cache entry", exception));
        }
    }
}
