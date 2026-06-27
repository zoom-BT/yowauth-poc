package yowyob.comops.api.kernel.application.port.in;

import reactor.core.publisher.Flux;

public interface ListClientApplicationPlansUseCase {
    Flux<ClientApplicationPlanView> listClientApplicationPlans();
}
