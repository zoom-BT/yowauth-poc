package yowyob.comops.api.auth.adapter.in.web;

import yowyob.comops.api.auth.domain.model.UserAccount;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserAccountResponse(
        UUID id,
        UUID tenantId,
        UUID actorId,
        String username,
        String email,
        String phoneNumber,
        String authProvider,
        String externalSubject,
        String status,
        String plan,
        String onboardingStatus,
        int onboardingStep,
        String accountType,
        String businessType,
        String onboardingPayload,
        boolean emailVerified,
        Instant emailVerifiedAt,
        boolean phoneVerified,
        Instant phoneVerifiedAt,
        boolean mfaEnabled,
        String mfaChannel,
        List<UserOrganizationAccessResponse> organizations) {

    public static UserAccountResponse from(UserAccount userAccount, List<UserOrganizationAccessResponse> organizations) {
        return new UserAccountResponse(
                userAccount.id(),
                userAccount.tenantId(),
                userAccount.actorId(),
                userAccount.username(),
                userAccount.email(),
                userAccount.phoneNumber(),
                userAccount.authProvider(),
                userAccount.externalSubject(),
                userAccount.status(),
                userAccount.plan(),
                userAccount.onboardingStatus(),
                userAccount.onboardingStep(),
                userAccount.accountType(),
                userAccount.businessType(),
                userAccount.onboardingPayload(),
                userAccount.emailVerified(),
                userAccount.emailVerifiedAt(),
                userAccount.phoneVerified(),
                userAccount.phoneVerifiedAt(),
                userAccount.mfaEnabled(),
                userAccount.mfaChannel(),
                organizations);
    }
}
