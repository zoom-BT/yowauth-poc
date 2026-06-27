package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record VerifyCaptchaRequest(
        @NotBlank String captchaToken,
        @NotBlank String answer) {
}
