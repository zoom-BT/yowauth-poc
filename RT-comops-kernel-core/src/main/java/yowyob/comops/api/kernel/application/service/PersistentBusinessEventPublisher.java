package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.kernel.application.port.out.BusinessEventPublisher;
import yowyob.comops.api.kernel.application.port.out.OutboxEventRepository;
import yowyob.comops.api.kernel.domain.model.BusinessEvent;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import yowyob.comops.api.kernel.domain.model.RequestCorrelation;
import yowyob.comops.api.kernel.domain.model.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PersistentBusinessEventPublisher implements BusinessEventPublisher {

    private final OutboxEventRepository outboxEventRepository;

    public PersistentBusinessEventPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public Mono<Void> publish(BusinessEvent event) {
        return Mono.deferContextual(ctx -> {
            Optional<RequestCorrelation> correlation = ReactiveRequestContextHolder.getCorrelation(ctx);
            String clientId = correlation.map(RequestCorrelation::clientApplicationId).orElse(null);
            String requestId = correlation.map(RequestCorrelation::requestId).orElse(null);
            UUID actorUserId = ctx.hasKey(RequestContextKeys.TENANT_CONTEXT)
                    ? ((TenantContext) ctx.get(RequestContextKeys.TENANT_CONTEXT)).userId()
                    : null;
            return outboxEventRepository.save(OutboxEvent.create(event, actorUserId, clientId, requestId)).then();
        });
    }
}
