package yowyob.comops.api.roles.application.port.in;

import yowyob.comops.api.roles.domain.model.UserRoleAssignment;
import reactor.core.publisher.Mono;

public interface AssignRoleToUserUseCase {
    Mono<UserRoleAssignment> assign(AssignRoleToUserCommand command);
}
