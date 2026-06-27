package yowyob.comops.api.kernel.application.port.in;

import yowyob.comops.api.kernel.domain.model.ClientApplication;
import reactor.core.publisher.Mono;

public interface UpdateClientApplicationUseCase {

    Mono<ClientApplication> update(UpdateClientApplicationCommand command);
}
