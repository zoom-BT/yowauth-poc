package yowyob.comops.api.roles.adapter.out.persistence;

import yowyob.comops.api.roles.application.port.out.RoleRepository;
import yowyob.comops.api.roles.domain.model.Role;
import yowyob.comops.api.roles.domain.model.RoleScopeType;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class RoleR2dbcRepositoryAdapter implements RoleRepository {

    private final RoleSpringDataRepository repository;

    public RoleR2dbcRepositoryAdapter(RoleSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Boolean> existsByCode(UUID tenantId, String code) {
        return repository.existsByTenantIdAndCodeIgnoreCase(tenantId, code);
    }

    @Override
    public Mono<Role> findById(UUID tenantId, UUID roleId) {
        return repository.findByIdAndTenantId(roleId, tenantId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Role> findByTenantId(UUID tenantId) {
        return repository.findAllByTenantId(tenantId).map(this::toDomain);
    }

    @Override
    public Mono<Role> save(Role role) {
        return repository.save(new RoleEntity(role.id(), role.tenantId(), role.createdAt(), role.updatedAt(), role.code(),
                role.name(), role.scopeType().name(), role.permissions())).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(UUID tenantId, UUID roleId) {
        return repository.deleteByIdAndTenantId(roleId, tenantId);
    }

    private Role toDomain(RoleEntity entity) {
        return Role.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(), entity.updatedAt(), entity.code(),
                entity.name(), RoleScopeType.from(entity.scopeType()), entity.permissions());
    }
}
