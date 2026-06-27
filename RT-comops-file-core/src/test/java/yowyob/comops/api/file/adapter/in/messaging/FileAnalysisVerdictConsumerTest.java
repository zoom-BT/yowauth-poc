package yowyob.comops.api.file.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import yowyob.comops.api.file.application.port.in.ApplyFileAnalysisVerdictUseCase;
import yowyob.comops.api.file.domain.model.FileAnalysisStatus;
import yowyob.comops.api.file.domain.model.FileBusinessEventType;
import yowyob.comops.api.kernel.domain.model.BusinessEvent;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;

@ExtendWith(MockitoExtension.class)
class FileAnalysisVerdictConsumerTest {

    @Mock
    private ApplyFileAnalysisVerdictUseCase applyVerdictUseCase;

    private OutboxEvent verdictEvent(UUID tenantId, UUID fileId, String verdict, String reason) {
        BusinessEvent event = BusinessEvent.now(tenantId, UUID.randomUUID(),
                FileBusinessEventType.FILE_ANALYSIS_COMPLETED, FileBusinessEventType.AGGREGATE_TYPE, fileId,
                Map.of("fileId", fileId.toString(), "verdict", verdict, "reason", reason));
        return OutboxEvent.create(event);
    }

    @Test
    void supportsOnlyAnalysisCompletedEvents() {
        FileAnalysisVerdictConsumer consumer = new FileAnalysisVerdictConsumer(applyVerdictUseCase);
        assertSupports(consumer, FileBusinessEventType.FILE_ANALYSIS_COMPLETED, true);
        assertSupports(consumer, FileBusinessEventType.FILE_UPLOADED, false);
    }

    private void assertSupports(FileAnalysisVerdictConsumer consumer, String eventType, boolean expected) {
        OutboxEvent event = OutboxEvent.create(BusinessEvent.now(UUID.randomUUID(), UUID.randomUUID(), eventType,
                FileBusinessEventType.AGGREGATE_TYPE, UUID.randomUUID(), Map.of()));
        org.assertj.core.api.Assertions.assertThat(consumer.supports(event)).isEqualTo(expected);
    }

    @Test
    void delegatesRejectedVerdictToUseCase() {
        FileAnalysisVerdictConsumer consumer = new FileAnalysisVerdictConsumer(applyVerdictUseCase);
        UUID tenantId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        when(applyVerdictUseCase.applyVerdict(eq(tenantId), eq(fileId), eq(FileAnalysisStatus.REJECTED), eq("virus")))
                .thenReturn(Mono.empty());

        StepVerifier.create(consumer.consume(verdictEvent(tenantId, fileId, "REJECTED", "virus")))
                .verifyComplete();

        verify(applyVerdictUseCase).applyVerdict(tenantId, fileId, FileAnalysisStatus.REJECTED, "virus");
    }

    @Test
    void failsWhenVerdictMissing() {
        FileAnalysisVerdictConsumer consumer = new FileAnalysisVerdictConsumer(applyVerdictUseCase);
        OutboxEvent event = OutboxEvent.create(BusinessEvent.now(UUID.randomUUID(), UUID.randomUUID(),
                FileBusinessEventType.FILE_ANALYSIS_COMPLETED, FileBusinessEventType.AGGREGATE_TYPE,
                UUID.randomUUID(), Map.of("fileId", UUID.randomUUID().toString())));

        StepVerifier.create(consumer.consume(event))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
