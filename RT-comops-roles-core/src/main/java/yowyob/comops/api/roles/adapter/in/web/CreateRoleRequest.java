package yowyob.comops.api.roles.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record CreateRoleRequest(@NotBlank String code, @NotBlank String name, String scopeType,
        @NotEmpty Set<String> permissions) {
}
