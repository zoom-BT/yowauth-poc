package yowyob.comops.api.kernel.application.port.in;

import yowyob.comops.api.kernel.application.service.OutboxEventSummaryView;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import yowyob.comops.api.kernel.domain.model.OutboxEventStatus;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GetOutboxObservabilityUseCase {

    Flux<OutboxEvent> listEvents(UUID tenantId, OutboxEventStatus status, int limit);

    Mono<OutboxEventSummaryView> summarizeOutbox(UUID tenantId);
}
