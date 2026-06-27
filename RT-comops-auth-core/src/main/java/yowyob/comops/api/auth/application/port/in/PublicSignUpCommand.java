package yowyob.comops.api.auth.application.port.in;

import java.util.UUID;

public record PublicSignUpCommand(
        UUID tenantId,
        String signUpSelectionToken,
        String contextId,
        String firstName,
        String lastName,
        String username,
        String email,
        String phoneNumber,
        String password,
        String socialProvider,
        String externalSubject,
        String captchaVerificationToken,
        String accountType,
        String businessType,
        String onboardingPayload) {

    public PublicSignUpCommand(UUID tenantId, String signUpSelectionToken, String contextId, String firstName,
            String lastName, String username, String email, String password) {
        this(tenantId, signUpSelectionToken, contextId, firstName, lastName, username, email, null, password, null,
                null, null, null, null, null);
    }
}
