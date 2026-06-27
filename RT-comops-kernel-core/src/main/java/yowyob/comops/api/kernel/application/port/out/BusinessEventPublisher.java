package yowyob.comops.api.kernel.application.port.out;

import yowyob.comops.api.kernel.domain.model.BusinessEvent;
import reactor.core.publisher.Mono;

public interface BusinessEventPublisher {

    Mono<Void> publish(BusinessEvent event);
}
