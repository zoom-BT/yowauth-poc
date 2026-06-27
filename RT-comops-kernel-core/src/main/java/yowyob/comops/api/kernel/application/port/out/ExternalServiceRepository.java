package yowyob.comops.api.kernel.application.port.out;

import yowyob.comops.api.kernel.domain.model.ExternalServiceDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Persistance du registre des services externes (catalogue extensible). */
public interface ExternalServiceRepository {

    Flux<ExternalServiceDefinition> findAll();

    Mono<ExternalServiceDefinition> save(ExternalServiceDefinition definition);

    Mono<Void> deactivate(String code);
}
