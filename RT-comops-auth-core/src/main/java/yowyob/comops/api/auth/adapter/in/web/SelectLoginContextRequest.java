package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record SelectLoginContextRequest(
        @NotBlank(message = "selectionToken is required") String selectionToken,
        @NotBlank(message = "contextId is required") String contextId,
        UUID organizationId) {
}
