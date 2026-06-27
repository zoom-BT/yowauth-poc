package yowyob.comops.api.auth.adapter.in.web;

import java.util.UUID;
import yowyob.comops.api.auth.application.service.AuthPasswordResetTokenService.PasswordResetContext;

public record PasswordResetContextResponse(
        String contextId,
        UUID tenantId,
        UUID userId,
        UUID actorId,
        String username,
        String email) {

    public static PasswordResetContextResponse from(PasswordResetContext context) {
        return new PasswordResetContextResponse(
                context.contextId(),
                context.tenantId(),
                context.userId(),
                context.actorId(),
                context.username(),
                context.email());
    }
}
