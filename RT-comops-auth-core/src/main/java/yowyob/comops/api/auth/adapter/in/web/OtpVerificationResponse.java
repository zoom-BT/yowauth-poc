package yowyob.comops.api.auth.adapter.in.web;

public record OtpVerificationResponse(
        boolean verified,
        String channel,
        String recipient,
        String purpose) {
}
