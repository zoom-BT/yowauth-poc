package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.domain.model.DomainEventProjection;
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
public class InMemoryDomainEventProjectionRepository implements DomainEventProjectionRepository {

    private final Map<UUID, DomainEventProjection> projections = new ConcurrentHashMap<>();

    @Override
    public Mono<DomainEventProjection> save(DomainEventProjection projection) {
        return Mono.fromSupplier(() -> {
            projections.put(projection.id(), projection);
            return projection;
        });
    }

    @Override
    public Mono<Boolean> existsBySourceEventId(UUID sourceEventId) {
        return Mono.fromSupplier(() -> projections.values().stream()
                .anyMatch(projection -> projection.sourceEventId().equals(sourceEventId)));
    }

    @Override
    public Flux<DomainEventProjection> findByTenantId(UUID tenantId) {
        return Flux.fromStream(projections.values().stream()
                .filter(projection -> projection.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(DomainEventProjection::createdAt)));
    }

    @Override
    public Flux<DomainEventProjection> findByTenantIdAndDomainType(UUID tenantId, String domainType, int limit) {
        String effectiveDomainType = domainType == null ? null : domainType.trim().toUpperCase();
        long effectiveLimit = limit <= 0 ? Long.MAX_VALUE : limit;
        return Flux.fromStream(projections.values().stream()
                .filter(projection -> projection.tenantId().equals(tenantId))
                .filter(projection -> effectiveDomainType == null || projection.domainType().equals(effectiveDomainType))
                .sorted(Comparator.comparing(DomainEventProjection::createdAt))
                .limit(effectiveLimit));
    }

    @Override
    public Mono<Long> countByTenantId(UUID tenantId) {
        return Mono.fromSupplier(() -> projections.values().stream()
                .filter(projection -> projection.tenantId().equals(tenantId))
                .count());
    }

    @Override
    public Mono<Long> countByTenantIdAndDomainType(UUID tenantId, String domainType) {
        String effectiveDomainType = domainType == null ? null : domainType.trim().toUpperCase();
        return Mono.fromSupplier(() -> projections.values().stream()
                .filter(projection -> projection.tenantId().equals(tenantId))
                .filter(projection -> effectiveDomainType == null || projection.domainType().equals(effectiveDomainType))
                .count());
    }

    @Override
    public Mono<Long> countAll() {
        return Mono.fromSupplier(() -> (long) projections.size());
    }

    @Override
    public Mono<java.time.Instant> findLatestOccurredAt() {
        return Mono.justOrEmpty(projections.values().stream()
                .map(DomainEventProjection::occurredAt)
                .max(Comparator.naturalOrder()));
    }

    @Override
    public Mono<java.time.Instant> findLatestCreatedAt() {
        return Mono.justOrEmpty(projections.values().stream()
                .map(DomainEventProjection::createdAt)
                .max(Comparator.naturalOrder()));
    }
}
