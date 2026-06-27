package yowyob.comops.api.roles.application.port.in;

import yowyob.comops.api.roles.domain.model.Role;
import java.util.UUID;
import reactor.core.publisher.Flux;

public interface ListRolesUseCase {
    Flux<Role> listRoles(UUID tenantId);
}
