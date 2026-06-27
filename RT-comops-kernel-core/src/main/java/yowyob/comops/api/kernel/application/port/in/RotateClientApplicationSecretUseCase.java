package yowyob.comops.api.kernel.application.port.in;

import yowyob.comops.api.kernel.application.service.ProvisionedClientApplication;
import reactor.core.publisher.Mono;

public interface RotateClientApplicationSecretUseCase {

    Mono<ProvisionedClientApplication> rotateSecret(RotateClientApplicationSecretCommand command);
}
