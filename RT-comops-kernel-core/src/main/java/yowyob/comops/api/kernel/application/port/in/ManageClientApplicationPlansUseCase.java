package yowyob.comops.api.kernel.application.port.in;

import reactor.core.publisher.Mono;

public interface ManageClientApplicationPlansUseCase {
    Mono<ClientApplicationPlanView> createClientApplicationPlan(SaveClientApplicationPlanCommand command);
    Mono<ClientApplicationPlanView> updateClientApplicationPlan(String planCode, SaveClientApplicationPlanCommand command);
    Mono<Void> deleteClientApplicationPlan(String planCode);
}
