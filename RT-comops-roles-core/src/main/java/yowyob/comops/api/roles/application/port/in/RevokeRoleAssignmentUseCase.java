package yowyob.comops.api.roles.application.port.in;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface RevokeRoleAssignmentUseCase {
    Mono<Void> revoke(UUID tenantId, UUID assignmentId);
}
