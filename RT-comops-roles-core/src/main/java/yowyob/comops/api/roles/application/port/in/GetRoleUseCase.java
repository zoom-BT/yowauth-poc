package yowyob.comops.api.roles.application.port.in;

import yowyob.comops.api.roles.domain.model.Role;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface GetRoleUseCase {
    Mono<Role> getRole(UUID tenantId, UUID roleId);
}
