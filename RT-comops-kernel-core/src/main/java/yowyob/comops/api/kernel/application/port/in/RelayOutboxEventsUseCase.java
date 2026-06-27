package yowyob.comops.api.kernel.application.port.in;

import reactor.core.publisher.Mono;

public interface RelayOutboxEventsUseCase {

    Mono<Integer> relayBatch(int batchSize);
}
