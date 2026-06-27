package yowyob.comops.api.roles.adapter.in.web;

import yowyob.comops.api.roles.domain.model.UserRoleAssignment;
import java.util.UUID;

public record UserRoleAssignmentResponse(UUID id, UUID tenantId, UUID userId, UUID roleId, String scopeType,
        UUID scopeId, String scope) {
    public static UserRoleAssignmentResponse from(UserRoleAssignment assignment) {
        return new UserRoleAssignmentResponse(assignment.id(), assignment.tenantId(), assignment.userId(),
                assignment.roleId(), assignment.scopeType().name(), assignment.scopeId(), assignment.scope());
    }
}
