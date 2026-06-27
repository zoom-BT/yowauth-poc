package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.kernel.application.port.out.OutboxEventRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import yowyob.comops.api.kernel.domain.model.OutboxEventStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryOutboxEventRepository implements OutboxEventRepository {

    private final Map<UUID, OutboxEvent> events = new ConcurrentHashMap<>();

    @Override
    public Mono<OutboxEvent> save(OutboxEvent event) {
        return Mono.fromSupplier(() -> {
            events.put(event.id(), event);
            return event;
        });
    }

    @Override
    public Flux<OutboxEvent> findByTenantId(UUID tenantId) {
        return Flux.fromStream(events.values().stream()
                .filter(event -> event.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(OutboxEvent::createdAt)));
    }

    @Override
    public Flux<OutboxEvent> findByTenantIdAndStatus(UUID tenantId, OutboxEventStatus status, int limit) {
        long effectiveLimit = limit <= 0 ? Long.MAX_VALUE : limit;
        return Flux.fromStream(events.values().stream()
                .filter(event -> event.tenantId().equals(tenantId))
                .filter(event -> event.status() == status)
                .sorted(Comparator.comparing(OutboxEvent::createdAt))
                .limit(effectiveLimit));
    }

    @Override
    public Flux<OutboxEvent> findByStatus(OutboxEventStatus status, int limit) {
        long effectiveLimit = limit <= 0 ? Long.MAX_VALUE : limit;
        return Flux.fromStream(events.values().stream()
                .filter(event -> event.status() == status)
                .sorted(Comparator.comparing(OutboxEvent::occurredAt))
                .limit(effectiveLimit));
    }

    @Override
    public Flux<OutboxEvent> findReadyForRelay(Instant asOf, int limit) {
        Instant effectiveAsOf = asOf == null ? Instant.now() : asOf;
        long effectiveLimit = limit <= 0 ? Long.MAX_VALUE : limit;
        return Flux.fromStream(events.values().stream()
                .filter(event -> event.isReadyForRelay(effectiveAsOf))
                .sorted(Comparator.comparing(OutboxEvent::createdAt))
                .limit(effectiveLimit));
    }

    @Override
    public Mono<Long> countByStatus(OutboxEventStatus status) {
        return Mono.fromSupplier(() -> events.values().stream()
                .filter(event -> event.status() == status)
                .count());
    }

    @Override
    public Mono<Long> countByTenantIdAndStatus(UUID tenantId, OutboxEventStatus status) {
        return Mono.fromSupplier(() -> events.values().stream()
                .filter(event -> event.tenantId().equals(tenantId))
                .filter(event -> event.status() == status)
                .count());
    }

    @Override
    public Mono<Instant> findOldestPendingOccurredAt() {
        return Mono.justOrEmpty(events.values().stream()
                .filter(event -> event.status() == OutboxEventStatus.PENDING)
                .map(OutboxEvent::occurredAt)
                .min(Comparator.naturalOrder()));
    }

    @Override
    public Mono<Instant> findLatestPublishedAt() {
        return Mono.justOrEmpty(events.values().stream()
                .filter(event -> event.status() == OutboxEventStatus.PUBLISHED)
                .map(OutboxEvent::publishedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder()));
    }
}
