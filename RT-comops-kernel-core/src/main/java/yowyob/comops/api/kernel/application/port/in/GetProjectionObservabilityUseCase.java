package yowyob.comops.api.kernel.application.port.in;

import yowyob.comops.api.kernel.application.service.DomainProjectionSummaryView;
import yowyob.comops.api.kernel.domain.model.DomainEventProjection;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GetProjectionObservabilityUseCase {

    Flux<DomainEventProjection> listProjections(UUID tenantId, String domainType, int limit);

    Mono<DomainProjectionSummaryView> summarizeProjections(UUID tenantId);
}
