package yowyob.comops.api.auth.adapter.in.web;

public record CaptchaVerificationResponse(
        String captchaVerificationToken,
        long expiresInSeconds) {
}
