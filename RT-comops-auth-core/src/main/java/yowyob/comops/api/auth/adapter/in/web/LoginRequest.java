package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String principal, @NotBlank String password) {
}
