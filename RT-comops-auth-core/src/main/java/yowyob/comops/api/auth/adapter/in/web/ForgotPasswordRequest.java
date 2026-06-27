package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank String principal) {
}
