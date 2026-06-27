package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.kernel.application.port.out.SystemAuditRepository;
import yowyob.comops.api.kernel.domain.model.SystemAuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class SystemAuditR2dbcRepositoryAdapter implements SystemAuditRepository {

    private final SystemAuditEntrySpringDataRepository repository;

    public SystemAuditR2dbcRepositoryAdapter(SystemAuditEntrySpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<SystemAuditEntry> save(SystemAuditEntry entry) {
        return repository.save(toEntity(entry)).map(this::toDomain);
    }

    @Override
    public Flux<SystemAuditEntry> findByTenantIdAndActorUserId(UUID tenantId, UUID actorUserId, int limit) {
        return repository.findTop200ByTenantIdAndActorUserIdOrderByCreatedAtDesc(tenantId, actorUserId)
                .take(limit)
                .map(this::toDomain);
    }

    @Override
    public Flux<SystemAuditEntry> findByTenantIdAndOrganizationId(UUID tenantId, UUID organizationId, int limit) {
        return repository.findTop200ByTenantIdAndOrganizationIdOrderByCreatedAtDesc(tenantId, organizationId)
                .take(limit)
                .map(this::toDomain);
    }

    @Override
    public Mono<List<SystemAuditEntry>> findFiltered(UUID tenantId, UUID organizationId, String action,
            UUID actorUserId, Instant from, Instant to, int page, int size) {
        long offset = (long) page * size;
        return repository.findFiltered(tenantId, organizationId, action, actorUserId, from, to, size, offset)
                .map(this::toDomain)
                .collectList();
    }

    @Override
    public Mono<Long> countFiltered(UUID tenantId, UUID organizationId, String action, UUID actorUserId,
            Instant from, Instant to) {
        return repository.countFiltered(tenantId, organizationId, action, actorUserId, from, to);
    }

    private SystemAuditEntryEntity toEntity(SystemAuditEntry entry) {
        return new SystemAuditEntryEntity(entry.id(), entry.tenantId(), entry.createdAt(), entry.updatedAt(),
                entry.organizationId(), entry.actorUserId(), entry.action(), entry.targetType(), entry.targetId(),
                entry.payloadSummary(), entry.requestId(), entry.clientApplicationId(), entry.remoteIp(),
                entry.httpMethod(), entry.httpPath(), entry.integrityHash());
    }

    private SystemAuditEntry toDomain(SystemAuditEntryEntity entity) {
        return SystemAuditEntry.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(), entity.updatedAt(),
                entity.organizationId(), entity.actorUserId(), entity.action(), entity.targetType(), entity.targetId(),
                entity.payloadSummary(), entity.requestId(), entity.clientApplicationId(), entity.remoteIp(),
                entity.httpMethod(), entity.httpPath(), entity.integrityHash());
    }
}
