package yowyob.comops.api.auth.adapter.in.web;

public record CaptchaChallengeResponse(
        String captchaToken,
        String prompt,
        String answerPreview,
        long expiresInSeconds) {
}
