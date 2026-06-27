package yowyob.comops.api.roles.adapter.out.persistence;
import yowyob.comops.api.roles.application.port.out.RoleRepository;
import yowyob.comops.api.roles.domain.model.Role;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryRoleRepository implements RoleRepository {

    private final Map<UUID, Role> roles = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> existsByCode(UUID tenantId, String code) {
        return Mono.fromSupplier(() -> roles.values().stream()
                .filter(role -> role.tenantId().equals(tenantId))
                .anyMatch(role -> role.code().equalsIgnoreCase(code)));
    }

    @Override
    public Mono<Role> findById(UUID tenantId, UUID roleId) {
        return Mono.justOrEmpty(roles.get(roleId))
                .filter(role -> role.tenantId().equals(tenantId));
    }

    @Override
    public Flux<Role> findByTenantId(UUID tenantId) {
        return Flux.fromStream(roles.values().stream()
                .filter(role -> role.tenantId().equals(tenantId)));
    }

    @Override
    public Mono<Role> save(Role role) {
        return Mono.fromSupplier(() -> {
            roles.put(role.id(), role);
            return role;
        });
    }

    @Override
    public Mono<Void> deleteById(UUID tenantId, UUID roleId) {
        return Mono.fromRunnable(() -> {
            Role existing = roles.get(roleId);
            if (existing != null && existing.tenantId().equals(tenantId)) {
                roles.remove(roleId);
            }
        });
    }
}
