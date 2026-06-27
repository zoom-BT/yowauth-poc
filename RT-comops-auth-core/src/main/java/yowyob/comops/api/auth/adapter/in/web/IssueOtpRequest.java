package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record IssueOtpRequest(
        @NotBlank String channel,
        @NotBlank String recipient,
        String purpose) {
}
