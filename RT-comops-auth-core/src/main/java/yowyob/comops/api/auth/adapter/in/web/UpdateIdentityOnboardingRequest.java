package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record UpdateIdentityOnboardingRequest(
        @NotBlank String accountType,
        String businessType,
        @Min(0) int step,
        String status,
        Map<String, Object> data) {
}
