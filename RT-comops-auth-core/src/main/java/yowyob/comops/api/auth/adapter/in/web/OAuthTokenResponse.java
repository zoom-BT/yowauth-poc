package yowyob.comops.api.auth.adapter.in.web;

public record OAuthTokenResponse(
        String access_token,
        String token_type,
        long expires_in,
        String scope,
        String issued_token_type) {
}
