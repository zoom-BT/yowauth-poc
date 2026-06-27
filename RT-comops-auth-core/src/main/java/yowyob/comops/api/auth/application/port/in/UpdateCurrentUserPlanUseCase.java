package yowyob.comops.api.auth.application.port.in;

import yowyob.comops.api.auth.domain.model.UserAccount;
import reactor.core.publisher.Mono;

public interface UpdateCurrentUserPlanUseCase {
    Mono<UserAccount> updateCurrentUserPlan(String plan);
}
