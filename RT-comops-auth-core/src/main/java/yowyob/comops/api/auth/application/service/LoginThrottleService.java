package yowyob.comops.api.auth.application.service;

import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.config.LoginThrottleProperties;

@Service
public class LoginThrottleService {

    private static final Logger LOG = LoggerFactory.getLogger(LoginThrottleService.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final LoginThrottleProperties properties;

    public LoginThrottleService(
            ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
            LoginThrottleProperties properties) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.properties = properties;
    }

    public Mono<Void> ensureNotThrottled(String principal, String remoteIp) {
        if (!properties.isEnabled() || redisTemplate == null) {
            return Mono.empty();
        }
        String principalLockKey = lockKey("p", normalize(principal));
        String ipLockKey = lockKey("i", remoteIp == null ? "unknown" : remoteIp);

        Mono<Boolean> principalLocked = redisTemplate.hasKey(principalLockKey);
        Mono<Boolean> ipLocked = redisTemplate.hasKey(ipLockKey);

        return Mono.zip(principalLocked, ipLocked)
                .flatMap(t -> {
                    if (Boolean.TRUE.equals(t.getT1())) {
                        return Mono.error(new LoginThrottledException("principal", properties.getPrincipalLockout()));
                    }
                    if (Boolean.TRUE.equals(t.getT2())) {
                        return Mono.error(new LoginThrottledException("ip", properties.getIpLockout()));
                    }
                    return Mono.empty();
                });
    }

    public Mono<Void> recordFailure(String principal, String remoteIp) {
        if (!properties.isEnabled() || redisTemplate == null) {
            return Mono.empty();
        }
        Mono<Void> byPrincipal = incrementAndMaybeLock(
                counterKey("p", normalize(principal)),
                lockKey("p", normalize(principal)),
                properties.getPrincipalWindow(),
                properties.getMaxFailuresPerPrincipal(),
                properties.getPrincipalLockout(),
                "principal=" + normalize(principal));
        Mono<Void> byIp = remoteIp == null ? Mono.empty() : incrementAndMaybeLock(
                counterKey("i", remoteIp),
                lockKey("i", remoteIp),
                properties.getIpWindow(),
                properties.getMaxFailuresPerIp(),
                properties.getIpLockout(),
                "ip=" + remoteIp);
        return Mono.when(byPrincipal, byIp);
    }

    public Mono<Void> recordSuccess(String principal) {
        if (!properties.isEnabled() || redisTemplate == null) {
            return Mono.empty();
        }
        String norm = normalize(principal);
        return redisTemplate.delete(counterKey("p", norm), lockKey("p", norm)).then();
    }

    private Mono<Void> incrementAndMaybeLock(
            String counterKey, String lockKey, Duration window, int threshold, Duration lockout, String tag) {
        return redisTemplate.opsForValue().increment(counterKey)
                .flatMap(count -> {
                    Mono<Void> ensureTtl = count == 1L
                            ? redisTemplate.expire(counterKey, window).then()
                            : Mono.empty();
                    if (count >= threshold) {
                        return ensureTtl
                                .then(redisTemplate.opsForValue().set(lockKey, Long.toString(count), lockout))
                                .doOnSuccess(v -> LOG.warn("Login throttle lock engaged ({}) count={} lockout={}s",
                                        tag, count, lockout.toSeconds()))
                                .then();
                    }
                    return ensureTtl;
                });
    }

    private String counterKey(String scope, String value) {
        return properties.getKeyPrefix() + ":fail:" + scope + ":" + value;
    }

    private String lockKey(String scope, String value) {
        return properties.getKeyPrefix() + ":lock:" + scope + ":" + value;
    }

    private String normalize(String principal) {
        return principal == null ? "unknown" : principal.trim().toLowerCase(Locale.ROOT);
    }

    public static class LoginThrottledException extends RuntimeException {
        private final String scope;
        private final Duration retryAfter;

        public LoginThrottledException(String scope, Duration retryAfter) {
            super("Login throttled at " + scope + " scope; retry after " + retryAfter);
            this.scope = scope;
            this.retryAfter = retryAfter;
        }

        public String getScope() { return scope; }
        public Duration getRetryAfter() { return retryAfter; }
    }
}
