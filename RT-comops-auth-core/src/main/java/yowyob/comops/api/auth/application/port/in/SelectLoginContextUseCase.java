package yowyob.comops.api.auth.application.port.in;

import reactor.core.publisher.Mono;

public interface SelectLoginContextUseCase {

    Mono<SelectedLoginContext> select(SelectLoginContextCommand command);
}
