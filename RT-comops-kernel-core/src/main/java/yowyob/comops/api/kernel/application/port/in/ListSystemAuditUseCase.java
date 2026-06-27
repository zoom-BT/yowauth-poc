package yowyob.comops.api.kernel.application.port.in;

import yowyob.comops.api.kernel.domain.model.SystemAuditEntry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface ListSystemAuditUseCase {
    Flux<SystemAuditEntry> listCurrentUserActivity(int limit);
    Flux<SystemAuditEntry> listCurrentOrganizationActivity(int limit);

    Mono<AuditPage> searchOrganizationActivity(UUID organizationId, String action, UUID actorUserId,
            Instant from, Instant to, int page, int size);

    record AuditPage(java.util.List<SystemAuditEntry> content, long totalElements, int page, int size, int totalPages) {
        public static AuditPage of(java.util.List<SystemAuditEntry> content, int page, int size, long total) {
            int totalPages = size == 0 ? 1 : (int) Math.ceil((double) total / size);
            return new AuditPage(content, total, page, size, totalPages);
        }
    }
}
