package yowyob.comops.api.kernel.application.port.in;

import yowyob.comops.api.kernel.domain.model.ClientApplication;
import reactor.core.publisher.Mono;

public interface AuthenticateClientApplicationUseCase {

    Mono<ClientApplication> authenticate(String clientId, String clientSecret);
}
