package yowyob.comops.api.auth.application.port.in;

import reactor.core.publisher.Mono;

public interface DiscoverSignUpContextsUseCase {

    Mono<DiscoverSignUpContextsResult> discover(DiscoverSignUpContextsCommand command);
}
