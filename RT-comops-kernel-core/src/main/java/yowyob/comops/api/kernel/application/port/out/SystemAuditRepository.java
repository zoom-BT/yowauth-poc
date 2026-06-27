package yowyob.comops.api.kernel.application.port.out;

import yowyob.comops.api.kernel.domain.model.SystemAuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SystemAuditRepository {
    Mono<SystemAuditEntry> save(SystemAuditEntry entry);
    Flux<SystemAuditEntry> findByTenantIdAndActorUserId(UUID tenantId, UUID actorUserId, int limit);
    Flux<SystemAuditEntry> findByTenantIdAndOrganizationId(UUID tenantId, UUID organizationId, int limit);
    Mono<List<SystemAuditEntry>> findFiltered(UUID tenantId, UUID organizationId, String action,
            UUID actorUserId, Instant from, Instant to, int page, int size);
    Mono<Long> countFiltered(UUID tenantId, UUID organizationId, String action, UUID actorUserId,
            Instant from, Instant to);
}
