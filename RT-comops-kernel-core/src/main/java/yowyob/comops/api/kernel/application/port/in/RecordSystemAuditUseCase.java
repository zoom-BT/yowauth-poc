package yowyob.comops.api.kernel.application.port.in;

import reactor.core.publisher.Mono;

public interface RecordSystemAuditUseCase {
    Mono<Void> record(java.util.UUID tenantId, java.util.UUID organizationId, java.util.UUID actorUserId,
            String action, String targetType, String targetId, String payloadSummary);
}
