package yowyob.comops.api.kernel.application.port.out;

import reactor.core.publisher.Mono;

public interface ReactiveTransactionalExecutor {

    <T> Mono<T> transactional(Mono<T> publisher);
}
