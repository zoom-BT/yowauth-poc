package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record ConfirmMfaLoginRequest(
        @NotBlank String mfaToken,
        @NotBlank String code) {
}
