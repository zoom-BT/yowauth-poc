package yowyob.comops.api.kernel.application.port.out;

import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import yowyob.comops.api.kernel.domain.model.OutboxEventStatus;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OutboxEventRepository {

    Mono<OutboxEvent> save(OutboxEvent event);

    Flux<OutboxEvent> findByTenantId(UUID tenantId);

    Flux<OutboxEvent> findByTenantIdAndStatus(UUID tenantId, OutboxEventStatus status, int limit);

    Flux<OutboxEvent> findByStatus(OutboxEventStatus status, int limit);

    Flux<OutboxEvent> findReadyForRelay(Instant asOf, int limit);

    Mono<Long> countByStatus(OutboxEventStatus status);

    Mono<Long> countByTenantIdAndStatus(UUID tenantId, OutboxEventStatus status);

    Mono<Instant> findOldestPendingOccurredAt();

    Mono<Instant> findLatestPublishedAt();
}
