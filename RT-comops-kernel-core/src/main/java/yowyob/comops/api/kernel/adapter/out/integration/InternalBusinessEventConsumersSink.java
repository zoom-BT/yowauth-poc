package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.application.port.out.BusinessEventConsumer;
import yowyob.comops.api.kernel.application.port.out.BusinessEventDeliverySink;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Order(0)
@ConditionalOnProperty(prefix = "iwm.outbox.consumers", name = "mode", havingValue = "inline", matchIfMissing = true)
public class InternalBusinessEventConsumersSink implements BusinessEventDeliverySink {

    private final List<BusinessEventConsumer> consumers;

    public InternalBusinessEventConsumersSink(List<BusinessEventConsumer> consumers) {
        this.consumers = consumers;
    }

    @Override
    public Mono<Void> deliver(OutboxEvent event) {
        return Flux.fromIterable(consumers)
                .filter(consumer -> consumer.supports(event))
                .concatMap(consumer -> consumer.consume(event))
                .then();
    }
}
