package yowyob.comops.api.auth.application.port.in;

import java.util.UUID;

public record LoginCommand(UUID tenantId, String principal, String password) {
}
