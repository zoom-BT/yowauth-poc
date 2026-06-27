package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.application.port.out.BusinessEventConsumer;
import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.domain.model.DomainEventProjection;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Set;
import reactor.core.publisher.Mono;

abstract class AbstractProjectionConsumer implements BusinessEventConsumer {

    private final DomainEventProjectionRepository repository;
    private final Set<String> supportedEventTypes;
    private final String domainType;

    protected AbstractProjectionConsumer(DomainEventProjectionRepository repository, Set<String> supportedEventTypes,
            String domainType) {
        this.repository = repository;
        this.supportedEventTypes = supportedEventTypes;
        this.domainType = domainType;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return supportedEventTypes.contains(event.eventType());
    }

    @Override
    public Mono<Void> consume(OutboxEvent event) {
        return repository.existsBySourceEventId(event.id())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.empty();
                    }
                    DomainEventProjection projection = DomainEventProjection.create(event, domainType, businessKey(event),
                            lifecycleStatus(event));
                    return repository.save(projection).then();
                });
    }

    protected abstract String businessKey(OutboxEvent event);

    protected abstract String lifecycleStatus(OutboxEvent event);
}
