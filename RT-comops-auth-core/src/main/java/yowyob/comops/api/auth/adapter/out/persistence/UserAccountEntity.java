package yowyob.comops.api.auth.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "auth_core", name = "user_account")
public record UserAccountEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        UUID actorId,
        String username,
        String email,
        String phoneNumber,
        String passwordHash,
        String authProvider,
        String externalSubject,
        String status,
        String plan,
        String onboardingStatus,
        int onboardingStep,
        String accountType,
        String businessType,
        String onboardingPayload,
        Instant emailVerifiedAt,
        Instant phoneVerifiedAt,
        boolean mfaEnabled,
        String mfaChannel) implements PersistableEntity {
}
