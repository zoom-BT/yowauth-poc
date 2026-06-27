package yowyob.comops.api.kernel.application.port.out;

import yowyob.comops.api.kernel.domain.model.ClientApplicationPlan;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientApplicationPlanRepository {
    Flux<ClientApplicationPlan> findAll();
    Mono<ClientApplicationPlan> findByCode(String code);
    Mono<Boolean> existsByCode(String code);
    Mono<ClientApplicationPlan> save(ClientApplicationPlan plan);
    Mono<Void> deleteByCode(String code);
}
