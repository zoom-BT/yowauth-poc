package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;

public record PublicSignUpRequest(
        UUID tenantId,
        String signUpSelectionToken,
        String contextId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String username,
        @Email @NotBlank String email,
        String phoneNumber,
        String password,
        String socialProvider,
        String externalSubject,
        String captchaVerificationToken,
        String accountType,
        String businessType,
        Map<String, Object> onboardingData) {
}
