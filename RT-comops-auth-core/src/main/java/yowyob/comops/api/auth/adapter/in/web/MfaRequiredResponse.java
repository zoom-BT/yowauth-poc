package yowyob.comops.api.auth.adapter.in.web;

public record MfaRequiredResponse(
        String nextStep,
        String mfaToken,
        String channel,
        String codePreview,
        long expiresInSeconds) {
}
