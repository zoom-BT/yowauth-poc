package yowyob.comops.api.file.adapter.in.messaging;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import yowyob.comops.api.file.application.port.in.ApplyFileAnalysisVerdictUseCase;
import yowyob.comops.api.file.domain.model.FileAnalysisStatus;
import yowyob.comops.api.file.domain.model.FileBusinessEventType;
import yowyob.comops.api.kernel.application.port.out.BusinessEventConsumer;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;

/**
 * Consumes {@code FILE_ANALYSIS_COMPLETED} events emitted by the external content-analysis service
 * and applies the verdict (ACCEPTED / REJECTED) to the corresponding quarantined file.
 *
 * <p>Expected payload: {@code {"fileId": "<uuid>", "verdict": "ACCEPTED|REJECTED", "reason": "..."}}.
 * The file id falls back to the event's aggregateId when the payload omits it.
 */
@Component
public class FileAnalysisVerdictConsumer implements BusinessEventConsumer {

    private final ApplyFileAnalysisVerdictUseCase applyVerdictUseCase;

    public FileAnalysisVerdictConsumer(ApplyFileAnalysisVerdictUseCase applyVerdictUseCase) {
        this.applyVerdictUseCase = applyVerdictUseCase;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return FileBusinessEventType.FILE_ANALYSIS_COMPLETED.equals(event.eventType());
    }

    @Override
    public Mono<Void> consume(OutboxEvent event) {
        Map<String, Object> payload = event.payload();
        UUID fileId = resolveFileId(payload, event.aggregateId());
        FileAnalysisStatus verdict = resolveVerdict(payload);
        if (fileId == null || verdict == null) {
            return Mono.error(new IllegalArgumentException(
                    "FILE_ANALYSIS_COMPLETED requires a fileId and a verdict (ACCEPTED|REJECTED)"));
        }
        String reason = stringValue(payload.get("reason"));
        return applyVerdictUseCase.applyVerdict(event.tenantId(), fileId, verdict, reason).then();
    }

    private static UUID resolveFileId(Map<String, Object> payload, UUID aggregateId) {
        String raw = stringValue(payload.get("fileId"));
        if (raw == null || raw.isBlank()) {
            return aggregateId;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return aggregateId;
        }
    }

    private static FileAnalysisStatus resolveVerdict(Map<String, Object> payload) {
        String raw = stringValue(payload.get("verdict"));
        if (raw == null) {
            raw = stringValue(payload.get("analysisStatus"));
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return FileAnalysisStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
