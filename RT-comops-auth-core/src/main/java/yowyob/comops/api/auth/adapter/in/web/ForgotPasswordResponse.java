package yowyob.comops.api.auth.adapter.in.web;

import java.util.List;
import yowyob.comops.api.auth.application.service.AuthPasswordResetTokenService.PasswordResetContext;

public record ForgotPasswordResponse(
        String principal,
        long matchingAccountCount,
        String selectionToken,
        long selectionTokenExpiresInSeconds,
        List<PasswordResetContextResponse> contexts) {

    public static ForgotPasswordResponse of(
            String principal,
            long matchingAccountCount,
            String selectionToken,
            long selectionTokenExpiresInSeconds,
            List<PasswordResetContext> contexts) {
        return new ForgotPasswordResponse(
                principal,
                matchingAccountCount,
                selectionToken,
                selectionTokenExpiresInSeconds,
                contexts.stream().map(PasswordResetContextResponse::from).toList());
    }
}
