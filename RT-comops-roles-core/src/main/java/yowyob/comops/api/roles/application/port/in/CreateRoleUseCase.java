package yowyob.comops.api.roles.application.port.in;

import yowyob.comops.api.roles.domain.model.Role;
import reactor.core.publisher.Mono;

public interface CreateRoleUseCase {

    Mono<Role> createRole(CreateRoleCommand command);
}
