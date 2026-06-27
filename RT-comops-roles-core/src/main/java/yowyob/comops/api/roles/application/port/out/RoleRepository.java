package yowyob.comops.api.roles.application.port.out;

import yowyob.comops.api.roles.domain.model.Role;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoleRepository {

    Mono<Boolean> existsByCode(UUID tenantId, String code);

    Mono<Role> findById(UUID tenantId, UUID roleId);

    Flux<Role> findByTenantId(UUID tenantId);

    Mono<Role> save(Role role);

    Mono<Void> deleteById(UUID tenantId, UUID roleId);
}
