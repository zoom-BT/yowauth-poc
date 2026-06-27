package yowyob.comops.api.roles.application.port.in;

import yowyob.comops.api.roles.domain.model.UserRoleAssignment;
import java.util.UUID;
import reactor.core.publisher.Flux;

public interface ListUserRoleAssignmentsUseCase {
    Flux<UserRoleAssignment> listAssignments(UUID tenantId, UUID userId);
}
