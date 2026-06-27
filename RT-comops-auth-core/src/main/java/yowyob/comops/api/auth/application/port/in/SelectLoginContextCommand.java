package yowyob.comops.api.auth.application.port.in;

import java.util.UUID;

public record SelectLoginContextCommand(
        String selectionToken,
        String contextId,
        UUID organizationId) {
}
