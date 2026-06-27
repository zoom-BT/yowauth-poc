package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.kernel.application.port.out.ClientApplicationPlanRepository;
import yowyob.comops.api.kernel.domain.model.ClientApplicationPlan;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class ClientApplicationPlanR2dbcRepositoryAdapter implements ClientApplicationPlanRepository {
    private final ClientApplicationPlanSpringDataRepository repository;

    public ClientApplicationPlanR2dbcRepositoryAdapter(ClientApplicationPlanSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Flux<ClientApplicationPlan> findAll() {
        return repository.findAll().map(this::toDomain);
    }

    @Override
    public Mono<ClientApplicationPlan> findByCode(String code) {
        return repository.findByCodeIgnoreCase(ClientApplicationPlan.normalizeCode(code)).map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByCode(String code) {
        return repository.existsByCodeIgnoreCase(ClientApplicationPlan.normalizeCode(code));
    }

    @Override
    public Mono<ClientApplicationPlan> save(ClientApplicationPlan plan) {
        return repository.save(toEntity(plan)).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteByCode(String code) {
        return repository.deleteByCodeIgnoreCase(ClientApplicationPlan.normalizeCode(code));
    }

    private ClientApplicationPlanEntity toEntity(ClientApplicationPlan plan) {
        return new ClientApplicationPlanEntity(plan.id(), plan.createdAt(), plan.updatedAt(), plan.code(),
                plan.displayName(), plan.description(), plan.allowedServices().toArray(String[]::new), plan.systemDefault());
    }

    private ClientApplicationPlan toDomain(ClientApplicationPlanEntity entity) {
        List<String> allowedServices = entity.allowedServices() == null ? List.of() : Arrays.asList(entity.allowedServices());
        return ClientApplicationPlan.rehydrate(entity.id(), entity.createdAt(), entity.updatedAt(), entity.code(),
                entity.displayName(), entity.description(), allowedServices, entity.systemDefault());
    }
}
