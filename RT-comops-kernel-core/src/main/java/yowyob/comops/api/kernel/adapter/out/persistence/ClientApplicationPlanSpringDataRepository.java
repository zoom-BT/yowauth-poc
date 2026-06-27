package yowyob.comops.api.kernel.adapter.out.persistence;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

@Profile("r2dbc")
interface ClientApplicationPlanSpringDataRepository extends ReactiveCrudRepository<ClientApplicationPlanEntity, UUID> {
    Mono<ClientApplicationPlanEntity> findByCodeIgnoreCase(String code);
    Mono<Boolean> existsByCodeIgnoreCase(String code);
    Mono<Void> deleteByCodeIgnoreCase(String code);
}
