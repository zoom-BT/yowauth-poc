package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record IssuePasswordResetRequest(
        @NotBlank String selectionToken,
        @NotBlank String contextId) {
}
