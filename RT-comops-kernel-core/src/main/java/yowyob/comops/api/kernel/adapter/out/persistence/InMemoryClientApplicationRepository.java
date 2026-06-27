package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.kernel.application.port.out.ClientApplicationRepository;
import yowyob.comops.api.kernel.domain.model.ClientApplication;
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
public class InMemoryClientApplicationRepository implements ClientApplicationRepository {

    private final Map<UUID, ClientApplication> storage = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> existsByClientId(String clientId) {
        return Mono.just(storage.values().stream()
                .anyMatch(clientApplication -> clientApplication.clientId().equalsIgnoreCase(clientId)));
    }

    @Override
    public Mono<ClientApplication> findById(UUID clientApplicationId) {
        return Mono.justOrEmpty(storage.get(clientApplicationId));
    }

    @Override
    public Mono<ClientApplication> findByClientId(String clientId) {
        return Mono.justOrEmpty(storage.values().stream()
                .filter(clientApplication -> clientApplication.clientId().equalsIgnoreCase(clientId))
                .findFirst());
    }

    @Override
    public Flux<ClientApplication> findAll() {
        return Flux.fromIterable(storage.values())
                .sort(Comparator.comparing(ClientApplication::clientId));
    }

    @Override
    public Mono<ClientApplication> save(ClientApplication clientApplication) {
        storage.put(clientApplication.id(), clientApplication);
        return Mono.just(clientApplication);
    }
}
