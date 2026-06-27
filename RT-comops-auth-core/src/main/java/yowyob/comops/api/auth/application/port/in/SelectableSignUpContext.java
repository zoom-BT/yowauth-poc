package yowyob.comops.api.auth.application.port.in;

import java.util.UUID;

public record SelectableSignUpContext(
        String contextId,
        UUID tenantId,
        UUID organizationId,
        String organizationCode,
        String organizationName,
        String organizationType) {
}
