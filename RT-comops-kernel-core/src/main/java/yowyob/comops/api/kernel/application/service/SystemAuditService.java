package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.kernel.application.port.in.ListSystemAuditUseCase;
import yowyob.comops.api.kernel.application.port.in.RecordSystemAuditUseCase;
import yowyob.comops.api.kernel.application.port.in.VerifyAuditIntegrityUseCase;
import yowyob.comops.api.kernel.application.port.out.BusinessEventPublisher;
import yowyob.comops.api.kernel.application.port.out.SystemAuditRepository;
import yowyob.comops.api.kernel.domain.model.BusinessEvent;
import yowyob.comops.api.kernel.domain.model.RequestCorrelation;
import yowyob.comops.api.kernel.domain.model.SystemAuditEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SystemAuditService implements RecordSystemAuditUseCase, ListSystemAuditUseCase, VerifyAuditIntegrityUseCase {

    private final SystemAuditRepository repository;
    private final AuditIntegrityHasher integrityHasher;
    private final BusinessEventPublisher businessEventPublisher;

    public SystemAuditService(SystemAuditRepository repository, AuditIntegrityHasher integrityHasher,
            BusinessEventPublisher businessEventPublisher) {
        this.repository = repository;
        this.integrityHasher = integrityHasher;
        this.businessEventPublisher = businessEventPublisher;
    }

    @Override
    public Mono<Void> record(UUID tenantId, UUID organizationId, UUID actorUserId,
            String action, String targetType, String targetId, String payloadSummary) {
        return ReactiveRequestContextHolder.getCorrelation()
                .defaultIfEmpty(Optional.empty())
                .flatMap(maybeCorrelation -> {
                    RequestCorrelation c = maybeCorrelation.orElse(null);
                    SystemAuditEntry entry = SystemAuditEntry.record(
                            tenantId, organizationId, actorUserId,
                            action, targetType, targetId, payloadSummary,
                            c == null ? null : c.requestId(),
                            c == null ? null : c.clientApplicationId(),
                            c == null ? null : c.remoteIp(),
                            c == null ? null : c.httpMethod(),
                            c == null ? null : c.requestPath());
                    String hash = integrityHasher.hash(entry);
                    SystemAuditEntry sealed = hash == null ? entry : entry.withIntegrityHash(hash);
                    return repository.save(sealed)
                            .flatMap(saved -> publishAuditAnchoringEvent(saved).thenReturn(saved))
                            .then();
                });
    }

    @Override
    public Flux<SystemAuditEntry> listCurrentUserActivity(int limit) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMapMany(context -> repository.findByTenantIdAndActorUserId(context.tenantId(), context.userId(),
                        sanitizeLimit(limit)));
    }

    @Override
    public Flux<SystemAuditEntry> listCurrentOrganizationActivity(int limit) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMapMany(context -> repository.findByTenantIdAndOrganizationId(context.tenantId(),
                        context.organizationId(), sanitizeLimit(limit)));
    }

    @Override
    public Mono<ListSystemAuditUseCase.AuditPage> searchOrganizationActivity(UUID organizationId,
            String action, UUID actorUserId, Instant from, Instant to, int page, int size) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> {
                    int safePage = Math.max(0, page);
                    int safeSize = Math.min(Math.max(1, size), 200);
                    Mono<List<SystemAuditEntry>> items = repository.findFiltered(
                            context.tenantId(), organizationId, action, actorUserId, from, to, safePage, safeSize);
                    Mono<Long> total = repository.countFiltered(
                            context.tenantId(), organizationId, action, actorUserId, from, to);
                    return Mono.zip(items, total,
                            (content, count) -> ListSystemAuditUseCase.AuditPage.of(content, safePage, safeSize, count));
                });
    }

    @Override
    public Mono<IntegrityReport> verify(UUID tenantId, UUID organizationId, Instant from, Instant to, int maxScan) {
        if (tenantId == null) {
            return Mono.error(new IllegalArgumentException("tenantId is required"));
        }
        int cap = maxScan <= 0 ? 1000 : Math.min(maxScan, 10_000);
        Instant scannedFrom = from == null ? Instant.EPOCH : from;
        Instant scannedTo = to == null ? Instant.now() : to;
        return repository.findFiltered(tenantId, organizationId, null, null, scannedFrom, scannedTo, 0, cap)
                .map(entries -> {
                    long scanned = entries.size();
                    long tampered = 0;
                    long missingHash = 0;
                    List<UUID> tamperedIds = new ArrayList<>();
                    for (SystemAuditEntry entry : entries) {
                        if (entry.integrityHash() == null) {
                            missingHash++;
                            continue;
                        }
                        if (!integrityHasher.isActive()) {
                            continue;
                        }
                        if (!integrityHasher.verify(entry, entry.integrityHash())) {
                            tampered++;
                            if (tamperedIds.size() < 20) {
                                tamperedIds.add(entry.id());
                            }
                        }
                    }
                    return new IntegrityReport(scanned, tampered, missingHash, tamperedIds,
                            scannedFrom, scannedTo, integrityHasher.isActive());
                });
    }

    private Mono<Void> publishAuditAnchoringEvent(SystemAuditEntry entry) {
        if (entry.tenantId() == null) {
            return Mono.empty();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("auditId", entry.id());
        payload.put("action", entry.action());
        payload.put("targetType", entry.targetType());
        payload.put("targetId", entry.targetId());
        payload.put("actorUserId", entry.actorUserId());
        payload.put("requestId", entry.requestId());
        payload.put("clientApplicationId", entry.clientApplicationId());
        payload.put("remoteIp", entry.remoteIp());
        payload.put("integrityHash", entry.integrityHash());
        return businessEventPublisher.publish(BusinessEvent.now(
                entry.tenantId(),
                entry.organizationId(),
                "SYSTEM_AUDIT_RECORDED",
                "SYSTEM_AUDIT",
                entry.id(),
                payload));
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }
}
