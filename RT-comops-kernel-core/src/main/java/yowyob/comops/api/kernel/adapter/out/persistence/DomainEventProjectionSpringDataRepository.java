package yowyob.comops.api.kernel.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DomainEventProjectionSpringDataRepository extends ReactiveCrudRepository<DomainEventProjectionEntity, UUID> {

    Mono<Boolean> existsBySourceEventId(UUID sourceEventId);

    Flux<DomainEventProjectionEntity> findAllByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Flux<DomainEventProjectionEntity> findAllByTenantIdAndDomainTypeOrderByCreatedAtAsc(UUID tenantId, String domainType);

    Mono<Long> countAllByTenantId(UUID tenantId);

    Mono<Long> countAllByTenantIdAndDomainType(UUID tenantId, String domainType);

    Mono<DomainEventProjectionEntity> findFirstByOrderByOccurredAtDesc();

    Mono<DomainEventProjectionEntity> findFirstByOrderByCreatedAtDesc();
}
