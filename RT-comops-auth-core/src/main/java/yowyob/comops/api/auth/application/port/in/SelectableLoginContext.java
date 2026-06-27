package yowyob.comops.api.auth.application.port.in;

import yowyob.comops.api.auth.application.port.out.UserOrganizationAccess;
import java.util.List;
import java.util.UUID;

public record SelectableLoginContext(
        String contextId,
        UUID tenantId,
        UUID userId,
        UUID actorId,
        List<UserOrganizationAccess> organizations) {
}
