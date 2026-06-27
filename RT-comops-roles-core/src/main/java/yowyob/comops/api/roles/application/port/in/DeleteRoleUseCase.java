package yowyob.comops.api.roles.application.port.in;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface DeleteRoleUseCase {
    Mono<Void> deleteRole(UUID tenantId, UUID roleId);
}
