package yowyob.comops.api.auth.adapter.in.web;

public record OtpChallengeResponse(
        String deliveryMode,
        String challengeToken,
        String codePreview,
        long expiresInSeconds) {
}
