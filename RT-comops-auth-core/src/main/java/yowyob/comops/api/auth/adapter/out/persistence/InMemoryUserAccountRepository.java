package yowyob.comops.api.auth.adapter.out.persistence;

import yowyob.comops.api.auth.application.port.out.UserAccountRepository;
import yowyob.comops.api.auth.domain.model.UserAccount;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<UUID, UserAccount> users = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> existsByUsername(UUID tenantId, String username) {
        return Mono.fromSupplier(() -> users.values().stream()
                .filter(user -> user.tenantId().equals(tenantId))
                .anyMatch(user -> user.username().equalsIgnoreCase(username)));
    }

    @Override
    public Mono<UserAccount> findById(UUID tenantId, UUID userId) {
        return Mono.justOrEmpty(users.get(userId)).filter(user -> user.tenantId().equals(tenantId));
    }

    @Override
    public Mono<UserAccount> findByPrincipal(UUID tenantId, String principal) {
        return Mono.fromSupplier(() -> users.values().stream()
                .filter(user -> user.tenantId().equals(tenantId))
                .filter(user -> user.username().equalsIgnoreCase(principal)
                        || user.email().equalsIgnoreCase(principal)
                        || (user.phoneNumber() != null && user.phoneNumber().equals(principal)))
                .findFirst()
                .orElse(null));
    }

    @Override
    public Flux<UserAccount> findAllByPrincipal(String principal) {
        return Flux.fromStream(users.values().stream()
                .filter(user -> user.username().equalsIgnoreCase(principal)
                        || user.email().equalsIgnoreCase(principal)
                        || (user.phoneNumber() != null && user.phoneNumber().equals(principal))));
    }

    @Override
    public Mono<UserAccount> save(UserAccount userAccount) {
        return Mono.fromSupplier(() -> {
            users.put(userAccount.id(), userAccount);
            return userAccount;
        });
    }
}
