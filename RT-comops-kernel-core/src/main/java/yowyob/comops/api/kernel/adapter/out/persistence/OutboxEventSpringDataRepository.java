package yowyob.comops.api.kernel.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OutboxEventSpringDataRepository extends ReactiveCrudRepository<OutboxEventEntity, UUID> {

    Flux<OutboxEventEntity> findAllByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Flux<OutboxEventEntity> findAllByTenantIdAndStatusOrderByCreatedAtAsc(UUID tenantId, String status);

    Flux<OutboxEventEntity> findAllByStatusOrderByOccurredAtAsc(String status);

    Flux<OutboxEventEntity> findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(String status, Instant asOf);

    Mono<Long> countAllByStatus(String status);

    Mono<Long> countAllByTenantIdAndStatus(UUID tenantId, String status);

    Mono<OutboxEventEntity> findFirstByStatusOrderByOccurredAtAsc(String status);

    Mono<OutboxEventEntity> findFirstByStatusOrderByPublishedAtDesc(String status);
}
