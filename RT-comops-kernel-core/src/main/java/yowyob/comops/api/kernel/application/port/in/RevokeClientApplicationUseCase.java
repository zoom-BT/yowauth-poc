package yowyob.comops.api.kernel.application.port.in;

import yowyob.comops.api.kernel.domain.model.ClientApplication;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface RevokeClientApplicationUseCase {

    Mono<ClientApplication> revoke(UUID clientApplicationId);
}
