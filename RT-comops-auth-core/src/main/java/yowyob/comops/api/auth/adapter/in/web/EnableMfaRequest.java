package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record EnableMfaRequest(@NotBlank String channel) {
}
