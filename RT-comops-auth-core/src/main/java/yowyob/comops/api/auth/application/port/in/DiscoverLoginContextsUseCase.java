package yowyob.comops.api.auth.application.port.in;

import reactor.core.publisher.Mono;

public interface DiscoverLoginContextsUseCase {

    Mono<DiscoverLoginContextsResult> discover(DiscoverLoginContextsCommand command);
}
