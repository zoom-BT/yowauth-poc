package yowyob.comops.api.auth.application.port.in;

import reactor.core.publisher.Mono;

public interface IdentifyAccountUseCase {

    Mono<IdentifyAccountResult> identify(IdentifyAccountCommand command);
}
