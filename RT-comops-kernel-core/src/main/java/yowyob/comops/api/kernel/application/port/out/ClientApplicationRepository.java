package yowyob.comops.api.kernel.application.port.out;

import yowyob.comops.api.kernel.domain.model.ClientApplication;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientApplicationRepository {

    Mono<Boolean> existsByClientId(String clientId);

    Mono<ClientApplication> findById(UUID clientApplicationId);

    Mono<ClientApplication> findByClientId(String clientId);

    Flux<ClientApplication> findAll();

    Mono<ClientApplication> save(ClientApplication clientApplication);
}
