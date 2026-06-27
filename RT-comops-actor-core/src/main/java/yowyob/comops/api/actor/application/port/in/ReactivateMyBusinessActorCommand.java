package yowyob.comops.api.actor.application.port.in;

import java.util.UUID;

public record ReactivateMyBusinessActorCommand(UUID tenantId, UUID userId) {
}
