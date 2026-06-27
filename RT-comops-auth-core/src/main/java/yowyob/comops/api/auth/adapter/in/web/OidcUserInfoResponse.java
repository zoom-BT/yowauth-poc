package yowyob.comops.api.auth.adapter.in.web;

import java.util.List;
import java.util.UUID;
import yowyob.comops.api.auth.application.service.AuthSharedSessionService;
import yowyob.comops.api.auth.application.service.AuthOidcService;

public record OidcUserInfoResponse(
        String sub,
        String sid,
        String preferredUsername,
        String email,
        UUID tenantId,
        UUID organizationId,
        UUID agencyId,
        UUID actorId,
        List<String> permissions,
        String clientId,
        String serviceCode,
        Boolean sso,
        List<OidcUserInfoContextResponse> contexts) {

    public static OidcUserInfoResponse from(AuthSharedSessionService.SharedSsoUserInfo userInfo) {
        return new OidcUserInfoResponse(
                userInfo.subject(),
                userInfo.sessionId(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                Boolean.TRUE,
                userInfo.contexts().stream().map(OidcUserInfoContextResponse::from).toList());
    }

    public static OidcUserInfoResponse from(AuthOidcService.OidcUserInfoPayload userInfo) {
        return new OidcUserInfoResponse(
                userInfo.sub(),
                userInfo.sid(),
                userInfo.preferredUsername(),
                userInfo.email(),
                userInfo.tenantId(),
                userInfo.organizationId(),
                userInfo.agencyId(),
                userInfo.actorId(),
                userInfo.permissions(),
                userInfo.clientId(),
                userInfo.serviceCode(),
                userInfo.sso(),
                userInfo.contexts().stream().map(OidcUserInfoContextResponse::from).toList());
    }
}
