package yowyob.comops.api.kernel.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface VerifyAuditIntegrityUseCase {

    Mono<IntegrityReport> verify(UUID tenantId, UUID organizationId, Instant from, Instant to, int maxScan);

    record IntegrityReport(
            long scanned,
            long tampered,
            long missingHash,
            List<UUID> tamperedIds,
            Instant scannedFrom,
            Instant scannedTo,
            boolean integrityEnabled) {
    }
}
