package yowyob.comops.api.kernel.application.port.out;

import yowyob.comops.api.kernel.domain.model.DomainEventProjection;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DomainEventProjectionRepository {

    Mono<DomainEventProjection> save(DomainEventProjection projection);

    Mono<Boolean> existsBySourceEventId(UUID sourceEventId);

    Flux<DomainEventProjection> findByTenantId(UUID tenantId);

    Flux<DomainEventProjection> findByTenantIdAndDomainType(UUID tenantId, String domainType, int limit);

    Mono<Long> countByTenantId(UUID tenantId);

    Mono<Long> countByTenantIdAndDomainType(UUID tenantId, String domainType);

    Mono<Long> countAll();

    Mono<java.time.Instant> findLatestOccurredAt();

    Mono<java.time.Instant> findLatestCreatedAt();
}
