package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank String challengeToken,
        @NotBlank String code,
        String purpose) {
}
