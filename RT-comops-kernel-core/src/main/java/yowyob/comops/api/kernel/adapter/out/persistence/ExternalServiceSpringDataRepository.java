package yowyob.comops.api.kernel.adapter.out.persistence;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

@Profile("r2dbc")
interface ExternalServiceSpringDataRepository extends ReactiveCrudRepository<ExternalServiceEntity, UUID> {
    Mono<ExternalServiceEntity> findByCodeIgnoreCase(String code);
}
