package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank String newPassword) {
}
