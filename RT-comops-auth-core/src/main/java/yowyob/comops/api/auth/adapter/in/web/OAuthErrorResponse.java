package yowyob.comops.api.auth.adapter.in.web;

public record OAuthErrorResponse(
        String error,
        String error_description) {
}
