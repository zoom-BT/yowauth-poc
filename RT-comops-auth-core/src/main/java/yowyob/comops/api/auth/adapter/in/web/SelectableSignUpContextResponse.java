package yowyob.comops.api.auth.adapter.in.web;

import java.util.UUID;
import yowyob.comops.api.auth.application.port.in.SelectableSignUpContext;

public record SelectableSignUpContextResponse(
        String contextId,
        UUID tenantId,
        UUID organizationId,
        String organizationCode,
        String organizationName,
        String organizationType) {

    public static SelectableSignUpContextResponse from(SelectableSignUpContext context) {
        return new SelectableSignUpContextResponse(
                context.contextId(),
                context.tenantId(),
                context.organizationId(),
                context.organizationCode(),
                context.organizationName(),
                context.organizationType());
    }
}
