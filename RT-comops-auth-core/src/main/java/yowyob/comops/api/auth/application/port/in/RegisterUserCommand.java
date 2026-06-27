package yowyob.comops.api.auth.application.port.in;

import java.util.UUID;

public record RegisterUserCommand(
        UUID tenantId,
        UUID actorId,
        String username,
        String email,
        String phoneNumber,
        String password,
        String authProvider,
        String externalSubject) {

    public RegisterUserCommand(UUID tenantId, UUID actorId, String username, String email, String password,
            String authProvider) {
        this(tenantId, actorId, username, email, null, password, authProvider, null);
    }
}
