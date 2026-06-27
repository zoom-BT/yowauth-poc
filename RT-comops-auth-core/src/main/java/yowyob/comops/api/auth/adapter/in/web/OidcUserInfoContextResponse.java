package yowyob.comops.api.auth.adapter.in.web;

import java.util.List;
import java.util.UUID;
import yowyob.comops.api.auth.application.service.AuthSharedSessionService;

public record OidcUserInfoContextResponse(
        String contextId,
        UUID tenantId,
        UUID userId,
        UUID actorId,
        List<UserOrganizationAccessResponse> organizations) {

    public static OidcUserInfoContextResponse from(AuthSharedSessionService.SharedSsoUserContext context) {
        return new OidcUserInfoContextResponse(
                context.contextId(),
                context.tenantId(),
                context.userId(),
                context.actorId(),
                context.organizations().stream().map(UserOrganizationAccessResponse::from).toList());
    }
}
