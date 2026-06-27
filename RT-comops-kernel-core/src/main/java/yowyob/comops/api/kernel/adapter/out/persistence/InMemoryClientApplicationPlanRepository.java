package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.kernel.application.port.out.ClientApplicationPlanRepository;
import yowyob.comops.api.kernel.domain.model.ClientApplicationPlan;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryClientApplicationPlanRepository implements ClientApplicationPlanRepository {
    private final Map<UUID, ClientApplicationPlan> storage = new ConcurrentHashMap<>();

    @Override
    public Flux<ClientApplicationPlan> findAll() {
        return Flux.fromIterable(storage.values()).sort(Comparator.comparing(ClientApplicationPlan::code));
    }

    @Override
    public Mono<ClientApplicationPlan> findByCode(String code) {
        String normalized = ClientApplicationPlan.normalizeCode(code);
        return Mono.justOrEmpty(storage.values().stream().filter(plan -> plan.code().equals(normalized)).findFirst());
    }

    @Override
    public Mono<Boolean> existsByCode(String code) {
        String normalized = ClientApplicationPlan.normalizeCode(code);
        return Mono.just(storage.values().stream().anyMatch(plan -> plan.code().equals(normalized)));
    }

    @Override
    public Mono<ClientApplicationPlan> save(ClientApplicationPlan plan) {
        storage.put(plan.id(), plan);
        return Mono.just(plan);
    }

    @Override
    public Mono<Void> deleteByCode(String code) {
        String normalized = ClientApplicationPlan.normalizeCode(code);
        storage.entrySet().removeIf(entry -> entry.getValue().code().equals(normalized));
        return Mono.empty();
    }
}
