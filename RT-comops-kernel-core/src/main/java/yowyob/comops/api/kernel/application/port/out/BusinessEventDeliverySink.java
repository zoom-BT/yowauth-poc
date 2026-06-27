package yowyob.comops.api.kernel.application.port.out;

import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import reactor.core.publisher.Mono;

public interface BusinessEventDeliverySink {

    Mono<Void> deliver(OutboxEvent event);
}
