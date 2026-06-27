package yowyob.comops.api.auth.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class UserAccount extends BaseEntity {

    private final UUID actorId;
    private final String username;
    private final String email;
    private final String phoneNumber;
    private final String passwordHash;
    private final String authProvider;
    private final String externalSubject;
    private final String status;
    private final String plan;
    private final String onboardingStatus;
    private final int onboardingStep;
    private final String accountType;
    private final String businessType;
    private final String onboardingPayload;
    private final Instant emailVerifiedAt;
    private final Instant phoneVerifiedAt;
    private final boolean mfaEnabled;
    private final String mfaChannel;

    private UserAccount(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID actorId, String username,
            String email, String phoneNumber, String passwordHash, String authProvider, String externalSubject,
            String status, String plan, String onboardingStatus, int onboardingStep, String accountType,
            String businessType, String onboardingPayload, Instant emailVerifiedAt, Instant phoneVerifiedAt,
            boolean mfaEnabled, String mfaChannel) {
        super(id, tenantId, createdAt, updatedAt);
        this.actorId = actorId;
        this.username = requireText(username, "username").toLowerCase(Locale.ROOT);
        this.email = requireText(email, "email").toLowerCase(Locale.ROOT);
        this.phoneNumber = normalizeNullableText(phoneNumber);
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.authProvider = requireText(authProvider, "authProvider").toUpperCase(Locale.ROOT);
        this.externalSubject = normalizeNullableText(externalSubject);
        this.status = requireText(status, "status").toUpperCase(Locale.ROOT);
        this.plan = requireText(plan, "plan").toUpperCase(Locale.ROOT);
        this.onboardingStatus = requireText(onboardingStatus, "onboardingStatus").toUpperCase(Locale.ROOT);
        if (onboardingStep < 0) {
            throw new IllegalArgumentException("onboardingStep must be greater than or equal to 0");
        }
        this.onboardingStep = onboardingStep;
        this.accountType = normalizeAccountType(accountType);
        this.businessType = normalizeNullableEnum(businessType);
        this.onboardingPayload = normalizeNullableText(onboardingPayload);
        this.emailVerifiedAt = emailVerifiedAt;
        this.phoneVerifiedAt = phoneVerifiedAt;
        this.mfaEnabled = mfaEnabled;
        this.mfaChannel = normalizeNullableEnum(mfaChannel);
    }

    private UserAccount(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID actorId, String username,
            String email, String passwordHash, String authProvider, String status, String plan,
            String onboardingStatus, int onboardingStep, Instant emailVerifiedAt) {
        this(id, tenantId, createdAt, updatedAt, actorId, username, email, null, passwordHash, authProvider, null,
                status, plan, onboardingStatus, onboardingStep, "PROSPECT", null, null, emailVerifiedAt, null,
                false, null);
    }

    public static UserAccount register(UUID tenantId, UUID actorId, String username, String email, String passwordHash,
            String authProvider) {
        return register(tenantId, actorId, username, email, passwordHash, authProvider, false, null);
    }

    /** Registers an account with explicit MFA configuration (used to bootstrap privileged admins). */
    public static UserAccount register(UUID tenantId, UUID actorId, String username, String email, String passwordHash,
            String authProvider, boolean mfaEnabled, String mfaChannel) {
        Instant now = Instant.now();
        return new UserAccount(UUID.randomUUID(), tenantId, now, now, actorId, username, email, null, passwordHash,
                authProvider, null, "ACTIVE", "FREE_TIER", "NOT_STARTED", 0, "PROSPECT", null, null,
                null, null, mfaEnabled, mfaChannel);
    }

    public static UserAccount rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID actorId,
            String username, String email, String passwordHash, String authProvider, String status, String plan,
            String onboardingStatus, int onboardingStep, Instant emailVerifiedAt) {
        return new UserAccount(id, tenantId, createdAt, updatedAt, actorId, username, email, passwordHash,
                authProvider, status, plan, onboardingStatus, onboardingStep, emailVerifiedAt);
    }

    public static UserAccount rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID actorId,
            String username, String email, String phoneNumber, String passwordHash, String authProvider,
            String externalSubject, String status, String plan, String onboardingStatus, int onboardingStep,
            String accountType, String businessType, String onboardingPayload, Instant emailVerifiedAt,
            Instant phoneVerifiedAt, boolean mfaEnabled, String mfaChannel) {
        return new UserAccount(id, tenantId, createdAt, updatedAt, actorId, username, email, phoneNumber, passwordHash,
                authProvider, externalSubject, status, plan, onboardingStatus, onboardingStep, accountType,
                businessType, onboardingPayload, emailVerifiedAt, phoneVerifiedAt, mfaEnabled, mfaChannel);
    }

    public UUID actorId() { return actorId; }
    public String username() { return username; }
    public String email() { return email; }
    public String phoneNumber() { return phoneNumber; }
    public String passwordHash() { return passwordHash; }
    public String authProvider() { return authProvider; }
    public String externalSubject() { return externalSubject; }
    public String status() { return status; }
    public String plan() { return plan; }
    public String onboardingStatus() { return onboardingStatus; }
    public int onboardingStep() { return onboardingStep; }
    public String accountType() { return accountType; }
    public String businessType() { return businessType; }
    public String onboardingPayload() { return onboardingPayload; }
    public Instant emailVerifiedAt() { return emailVerifiedAt; }
    public Instant phoneVerifiedAt() { return phoneVerifiedAt; }
    public boolean emailVerified() { return emailVerifiedAt != null; }
    public boolean phoneVerified() { return phoneVerifiedAt != null; }
    public boolean mfaEnabled() { return mfaEnabled; }
    public String mfaChannel() { return mfaChannel; }

    public UserAccount updatePlan(String plan) {
        return copy(phoneNumber, passwordHash, authProvider, externalSubject, status, requireText(plan, "plan"),
                onboardingStatus, onboardingStep, accountType, businessType, onboardingPayload, emailVerifiedAt,
                phoneVerifiedAt, mfaEnabled, mfaChannel);
    }

    public UserAccount updateOnboarding(int onboardingStep, String onboardingStatus) {
        return copy(phoneNumber, passwordHash, authProvider, externalSubject, status, plan,
                onboardingStatus == null || onboardingStatus.isBlank() ? this.onboardingStatus : onboardingStatus,
                onboardingStep, accountType, businessType, onboardingPayload, emailVerifiedAt, phoneVerifiedAt,
                mfaEnabled, mfaChannel);
    }

    public UserAccount updatePassword(String nextPasswordHash) {
        return copy(phoneNumber, requireText(nextPasswordHash, "passwordHash"), authProvider, externalSubject, status,
                plan, onboardingStatus, onboardingStep, accountType, businessType, onboardingPayload, emailVerifiedAt,
                phoneVerifiedAt, mfaEnabled, mfaChannel);
    }

    public UserAccount markEmailVerified() {
        if (emailVerifiedAt != null) {
            return this;
        }
        return copy(phoneNumber, passwordHash, authProvider, externalSubject, status, plan, onboardingStatus,
                onboardingStep, accountType, businessType, onboardingPayload, Instant.now(), phoneVerifiedAt,
                mfaEnabled, mfaChannel);
    }

    public UserAccount updatePhoneNumber(String phoneNumber) {
        return copy(phoneNumber, passwordHash, authProvider, externalSubject, status, plan, onboardingStatus,
                onboardingStep, accountType, businessType, onboardingPayload, emailVerifiedAt, null, mfaEnabled,
                mfaChannel);
    }

    public UserAccount markPhoneVerified() {
        return copy(phoneNumber, passwordHash, authProvider, externalSubject, status, plan, onboardingStatus,
                onboardingStep, accountType, businessType, onboardingPayload, emailVerifiedAt, Instant.now(),
                mfaEnabled, mfaChannel);
    }

    public UserAccount enableMfa(String channel) {
        return copy(phoneNumber, passwordHash, authProvider, externalSubject, status, plan, onboardingStatus,
                onboardingStep, accountType, businessType, onboardingPayload, emailVerifiedAt, phoneVerifiedAt,
                true, requireText(channel, "mfaChannel").toUpperCase(Locale.ROOT));
    }

    public UserAccount disableMfa() {
        return copy(phoneNumber, passwordHash, authProvider, externalSubject, status, plan, onboardingStatus,
                onboardingStep, accountType, businessType, onboardingPayload, emailVerifiedAt, phoneVerifiedAt,
                false, null);
    }

    public UserAccount updateIdentityProfile(String accountType, String businessType, String onboardingPayload,
            int onboardingStep, String onboardingStatus) {
        return copy(phoneNumber, passwordHash, authProvider, externalSubject, status, plan,
                onboardingStatus == null || onboardingStatus.isBlank() ? this.onboardingStatus : onboardingStatus,
                onboardingStep, accountType, businessType, onboardingPayload, emailVerifiedAt, phoneVerifiedAt,
                mfaEnabled, mfaChannel);
    }

    public UserAccount linkExternalIdentity(String authProvider, String externalSubject) {
        return copy(phoneNumber, passwordHash, authProvider, externalSubject, status, plan, onboardingStatus,
                onboardingStep, accountType, businessType, onboardingPayload, emailVerifiedAt, phoneVerifiedAt,
                mfaEnabled, mfaChannel);
    }

    public UserAccount markForInitialPersistence() {
        return new UserAccount(id(), tenantId(), createdAt(), createdAt(), actorId, username, email, phoneNumber,
                passwordHash, authProvider, externalSubject, status, plan, onboardingStatus, onboardingStep,
                accountType, businessType, onboardingPayload, emailVerifiedAt, phoneVerifiedAt, mfaEnabled,
                mfaChannel);
    }

    private UserAccount copy(String phoneNumber, String passwordHash, String authProvider, String externalSubject,
            String status, String plan, String onboardingStatus, int onboardingStep, String accountType,
            String businessType, String onboardingPayload, Instant emailVerifiedAt, Instant phoneVerifiedAt,
            boolean mfaEnabled, String mfaChannel) {
        return new UserAccount(id(), tenantId(), createdAt(), Instant.now(), actorId, username, email, phoneNumber,
                passwordHash, authProvider, externalSubject, status, plan, onboardingStatus, onboardingStep,
                accountType, businessType, onboardingPayload, emailVerifiedAt, phoneVerifiedAt, mfaEnabled,
                mfaChannel);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeAccountType(String value) {
        if (value == null || value.isBlank()) {
            return "PROSPECT";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (!normalized.equals("PROSPECT") && !normalized.equals("BUSINESS")) {
            throw new IllegalArgumentException("accountType must be PROSPECT or BUSINESS");
        }
        return normalized;
    }

    private static String normalizeNullableEnum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }
}
