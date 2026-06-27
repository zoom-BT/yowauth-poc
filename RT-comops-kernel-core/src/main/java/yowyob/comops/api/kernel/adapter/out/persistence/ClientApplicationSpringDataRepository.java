package yowyob.comops.api.kernel.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ClientApplicationSpringDataRepository extends ReactiveCrudRepository<ClientApplicationEntity, UUID> {

    Mono<Boolean> existsByClientIdIgnoreCase(String clientId);

    Mono<ClientApplicationEntity> findByClientIdIgnoreCase(String clientId);
}
