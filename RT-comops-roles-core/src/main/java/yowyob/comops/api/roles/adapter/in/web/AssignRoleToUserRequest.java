package yowyob.comops.api.roles.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignRoleToUserRequest(@NotNull UUID userId, @NotNull UUID roleId, String scopeType, UUID scopeId,
        @NotBlank String scope) {
}
