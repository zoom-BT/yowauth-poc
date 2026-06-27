package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.kernel.application.port.out.SystemAuditRepository;
import yowyob.comops.api.kernel.domain.model.SystemAuditEntry;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemorySystemAuditRepository implements SystemAuditRepository {

    private final Map<UUID, SystemAuditEntry> entries = new ConcurrentHashMap<>();

    @Override
    public Mono<SystemAuditEntry> save(SystemAuditEntry entry) {
        return Mono.fromSupplier(() -> {
            entries.put(entry.id(), entry);
            return entry;
        });
    }

    @Override
    public Flux<SystemAuditEntry> findByTenantIdAndActorUserId(UUID tenantId, UUID actorUserId, int limit) {
        return Flux.fromStream(entries.values().stream()
                .filter(entry -> entry.tenantId().equals(tenantId))
                .filter(entry -> actorUserId != null && actorUserId.equals(entry.actorUserId()))
                .sorted(Comparator.comparing(SystemAuditEntry::createdAt).reversed())
                .limit(limit));
    }

    @Override
    public Flux<SystemAuditEntry> findByTenantIdAndOrganizationId(UUID tenantId, UUID organizationId, int limit) {
        return Flux.fromStream(entries.values().stream()
                .filter(entry -> entry.tenantId().equals(tenantId))
                .filter(entry -> organizationId != null && organizationId.equals(entry.organizationId()))
                .sorted(Comparator.comparing(SystemAuditEntry::createdAt).reversed())
                .limit(limit));
    }

    @Override
    public Mono<List<SystemAuditEntry>> findFiltered(UUID tenantId, UUID organizationId, String action,
            UUID actorUserId, Instant from, Instant to, int page, int size) {
        return Mono.fromSupplier(() -> filteredStream(tenantId, organizationId, action, actorUserId, from, to)
                .skip((long) page * size)
                .limit(size)
                .toList());
    }

    @Override
    public Mono<Long> countFiltered(UUID tenantId, UUID organizationId, String action, UUID actorUserId,
            Instant from, Instant to) {
        return Mono.fromSupplier(() -> filteredStream(tenantId, organizationId, action, actorUserId, from, to).count());
    }

    private Stream<SystemAuditEntry> filteredStream(UUID tenantId, UUID organizationId, String action,
            UUID actorUserId, Instant from, Instant to) {
        return entries.values().stream()
                .filter(e -> e.tenantId().equals(tenantId))
                .filter(e -> organizationId == null || organizationId.equals(e.organizationId()))
                .filter(e -> action == null || action.equals(e.action()))
                .filter(e -> actorUserId == null || actorUserId.equals(e.actorUserId()))
                .filter(e -> from == null || !e.createdAt().isBefore(from))
                .filter(e -> to == null || !e.createdAt().isAfter(to))
                .sorted(Comparator.comparing(SystemAuditEntry::createdAt).reversed());
    }
}
