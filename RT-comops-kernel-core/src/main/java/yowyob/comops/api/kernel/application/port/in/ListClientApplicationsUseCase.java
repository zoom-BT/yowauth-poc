package yowyob.comops.api.kernel.application.port.in;

import yowyob.comops.api.kernel.domain.model.ClientApplication;
import reactor.core.publisher.Flux;

public interface ListClientApplicationsUseCase {

    Flux<ClientApplication> list();
}
