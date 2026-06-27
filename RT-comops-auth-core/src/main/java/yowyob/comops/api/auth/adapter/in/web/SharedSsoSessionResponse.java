package yowyob.comops.api.auth.adapter.in.web;

import yowyob.comops.api.auth.application.service.AuthSsoSessionTokenService;

public record SharedSsoSessionResponse(
        String token,
        String tokenType,
        long expiresInSeconds) {

    public static SharedSsoSessionResponse from(AuthSsoSessionTokenService.IssuedSsoSessionToken issued) {
        return new SharedSsoSessionResponse(issued.token(), "Bearer", issued.expiresInSeconds());
    }
}
