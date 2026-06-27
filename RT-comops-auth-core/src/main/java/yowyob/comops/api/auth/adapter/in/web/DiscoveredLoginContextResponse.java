package yowyob.comops.api.auth.adapter.in.web;

import yowyob.comops.api.auth.application.port.in.SelectableLoginContext;
import java.util.List;
import java.util.UUID;

public record DiscoveredLoginContextResponse(
        String contextId,
        UUID tenantId,
        UUID userId,
        UUID actorId,
        List<UserOrganizationAccessResponse> organizations) {

    public static DiscoveredLoginContextResponse from(SelectableLoginContext context) {
        return new DiscoveredLoginContextResponse(
                context.contextId(),
                context.tenantId(),
                context.userId(),
                context.actorId(),
                context.organizations().stream()
                        .map(UserOrganizationAccessResponse::from)
                        .toList());
    }
}
