package yowyob.comops.api.kernel.application.port.out;

import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import reactor.core.publisher.Mono;

public interface BusinessEventConsumer {

    boolean supports(OutboxEvent event);

    Mono<Void> consume(OutboxEvent event);
}
