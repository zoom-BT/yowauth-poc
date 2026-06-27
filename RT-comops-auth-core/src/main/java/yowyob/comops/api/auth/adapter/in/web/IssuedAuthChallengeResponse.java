package yowyob.comops.api.auth.adapter.in.web;

public record IssuedAuthChallengeResponse(
        String deliveryMode,
        String challengeTokenPreview,
        long expiresInSeconds) {

    public static IssuedAuthChallengeResponse preview(String token, long expiresInSeconds) {
        return new IssuedAuthChallengeResponse("PREVIEW_ONLY", token, expiresInSeconds);
    }
}
