package yowyob.comops.api.actor.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class BusinessActorProfile extends BaseEntity implements BusinessActor {

    private final UUID actorId;
    private final BusinessActorGovernanceStatus governanceStatus;
    private final UUID governedByUserId;
    private final Instant governedAt;
    private final String governanceReason;
    private final String code;
    private final boolean isIndividual;
    private final boolean isAvailable;
    private final boolean isVerified;
    private final boolean isActive;
    private final String type;
    private final String role;
    private final Set<String> qualifications;
    private final Set<String> paymentMethods;
    private final Set<UUID> addresses;
    private final String biography;
    private final Instant deletedAt;
    private final String name;
    private final String businessId;
    private final String niu;
    private final String tradeRegistryNumber;
    private final String website;
    private final String contactPhone;
    private final String privateAddress;
    private final String businessAddress;
    private final String businessProfile;

    private BusinessActorProfile(
            UUID id,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt,
            UUID actorId,
            BusinessActorGovernanceStatus governanceStatus,
            UUID governedByUserId,
            Instant governedAt,
            String governanceReason,
            String code,
            boolean isIndividual,
            boolean isAvailable,
            boolean isVerified,
            boolean isActive,
            String type,
            String role,
            Set<String> qualifications,
            Set<String> paymentMethods,
            Set<UUID> addresses,
            String biography,
            Instant deletedAt,
            String name,
            String businessId,
            String niu,
            String tradeRegistryNumber,
            String website,
            String contactPhone,
            String privateAddress,
            String businessAddress,
            String businessProfile) {
        super(id, tenantId, createdAt, updatedAt);
        this.actorId = requireUuid(actorId, "actorId");
        this.governanceStatus = governanceStatus == null ? BusinessActorGovernanceStatus.PENDING_REVIEW : governanceStatus;
        this.governedByUserId = governedByUserId;
        this.governedAt = governedAt;
        this.governanceReason = normalize(governanceReason);
        this.code = normalizeCode(code, businessId, name, actorId);
        this.isIndividual = isIndividual;
        this.isAvailable = isAvailable;
        this.isVerified = isVerified;
        this.isActive = isActive;
        this.type = normalizeType(type);
        this.role = normalizeRole(role);
        this.qualifications = normalizeStringSet(qualifications);
        this.paymentMethods = normalizeStringSet(paymentMethods);
        this.addresses = normalizeUuidSet(addresses);
        this.biography = normalize(firstNonBlank(biography, businessProfile));
        this.deletedAt = deletedAt;
        this.name = requireText(name, "name");
        this.businessId = normalize(businessId);
        this.niu = normalize(niu);
        this.tradeRegistryNumber = normalize(tradeRegistryNumber);
        this.website = normalize(website);
        this.contactPhone = normalize(contactPhone);
        this.privateAddress = normalize(privateAddress);
        this.businessAddress = normalize(businessAddress);
        this.businessProfile = normalize(businessProfile);
    }

    public static BusinessActorProfile create(UUID tenantId, UUID actorId, String name, String businessId, String niu,
            String tradeRegistryNumber, String website, String contactPhone, String privateAddress,
            String businessAddress, String businessProfile) {
        return create(tenantId, actorId, null, false, true, false, true, null, null, Set.of(), Set.of(), Set.of(),
                businessProfile, name, businessId, niu, tradeRegistryNumber, website, contactPhone, privateAddress,
                businessAddress, businessProfile);
    }

    public static BusinessActorProfile create(UUID tenantId, UUID actorId, String code, boolean isIndividual,
            boolean isAvailable, boolean isVerified, boolean isActive, String type, String role,
            Set<String> qualifications, Set<String> paymentMethods, Set<UUID> addresses, String biography,
            String name, String businessId, String niu, String tradeRegistryNumber, String website,
            String contactPhone, String privateAddress, String businessAddress, String businessProfile) {
        Instant now = Instant.now();
        return new BusinessActorProfile(UUID.randomUUID(), tenantId, now, now, actorId,
                BusinessActorGovernanceStatus.PENDING_REVIEW, null, null, null, code, isIndividual, isAvailable,
                isVerified, isActive, type, role, qualifications, paymentMethods, addresses, biography, null, name,
                businessId, niu, tradeRegistryNumber, website, contactPhone, privateAddress, businessAddress,
                businessProfile);
    }

    public static BusinessActorProfile rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID actorId, String governanceStatus, UUID governedByUserId, Instant governedAt, String governanceReason,
            String name, String businessId, String niu, String tradeRegistryNumber, String website,
            String contactPhone, String privateAddress, String businessAddress, String businessProfile) {
        return rehydrate(id, tenantId, createdAt, updatedAt, actorId, governanceStatus, governedByUserId, governedAt,
                governanceReason, null, false, true, false, true, null, null, Set.of(), Set.of(), Set.of(),
                businessProfile, null, name, businessId, niu, tradeRegistryNumber, website, contactPhone,
                privateAddress, businessAddress, businessProfile);
    }

    public static BusinessActorProfile rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID actorId, String governanceStatus, UUID governedByUserId, Instant governedAt, String governanceReason,
            String code, boolean isIndividual, boolean isAvailable, boolean isVerified, boolean isActive, String type,
            String role, Set<String> qualifications, Set<String> paymentMethods, Set<UUID> addresses,
            String biography, Instant deletedAt, String name, String businessId, String niu,
            String tradeRegistryNumber, String website, String contactPhone, String privateAddress,
            String businessAddress, String businessProfile) {
        return new BusinessActorProfile(id, tenantId, createdAt, updatedAt, actorId,
                BusinessActorGovernanceStatus.from(governanceStatus), governedByUserId, governedAt, governanceReason,
                code, isIndividual, isAvailable, isVerified, isActive, type, role, qualifications, paymentMethods,
                addresses, biography, deletedAt, name, businessId, niu, tradeRegistryNumber, website, contactPhone,
                privateAddress, businessAddress, businessProfile);
    }

    public BusinessActorProfile update(String name, String businessId, String niu, String tradeRegistryNumber,
            String website, String contactPhone, String privateAddress, String businessAddress, String businessProfile) {
        return update(code, isIndividual, isAvailable, isVerified, isActive, type, role, qualifications,
                paymentMethods, addresses, biography, name, businessId, niu, tradeRegistryNumber, website,
                contactPhone, privateAddress, businessAddress, businessProfile);
    }

    public BusinessActorProfile update(String code, boolean isIndividual, boolean isAvailable, boolean isVerified,
            boolean isActive, String type, String role, Set<String> qualifications, Set<String> paymentMethods,
            Set<UUID> addresses, String biography, String name, String businessId, String niu,
            String tradeRegistryNumber, String website, String contactPhone, String privateAddress,
            String businessAddress, String businessProfile) {
        return new BusinessActorProfile(id(), tenantId(), createdAt(), Instant.now(), actorId,
                governanceStatus, governedByUserId, governedAt, governanceReason, code, isIndividual, isAvailable,
                isVerified, isActive, type, role, qualifications, paymentMethods, addresses, biography, deletedAt,
                name, businessId, niu, tradeRegistryNumber, website, contactPhone, privateAddress, businessAddress,
                businessProfile);
    }

    public BusinessActorProfile approve(UUID adminUserId, String reason) {
        return govern(BusinessActorGovernanceStatus.APPROVED, adminUserId, reason, true, isActive);
    }

    public BusinessActorProfile reject(UUID adminUserId, String reason) {
        return govern(BusinessActorGovernanceStatus.REJECTED, adminUserId, reason, false, false);
    }

    public BusinessActorProfile suspend(UUID adminUserId, String reason) {
        if (governanceStatus == BusinessActorGovernanceStatus.BLOCKED) {
            throw new IllegalStateException("blocked business actor cannot be suspended");
        }
        return govern(BusinessActorGovernanceStatus.SUSPENDED, adminUserId, reason, isVerified, false);
    }

    public BusinessActorProfile block(UUID adminUserId, String reason) {
        return govern(BusinessActorGovernanceStatus.BLOCKED, adminUserId, reason, isVerified, false);
    }

    public BusinessActorProfile reactivate(UUID adminUserId, String reason) {
        return govern(BusinessActorGovernanceStatus.APPROVED, adminUserId, reason, true, true);
    }

    private BusinessActorProfile govern(BusinessActorGovernanceStatus status, UUID adminUserId, String reason,
            boolean verified, boolean active) {
        return new BusinessActorProfile(id(), tenantId(), createdAt(), Instant.now(), actorId,
                status, adminUserId, Instant.now(), reason, code, isIndividual, isAvailable, verified, active,
                type, role, qualifications, paymentMethods, addresses, biography, deletedAt, name, businessId, niu,
                tradeRegistryNumber, website, contactPhone, privateAddress, businessAddress, businessProfile);
    }

    public UUID actorId() { return actorId; }
    public BusinessActorGovernanceStatus governanceStatus() { return governanceStatus; }
    public UUID governedByUserId() { return governedByUserId; }
    public Instant governedAt() { return governedAt; }
    public String governanceReason() { return governanceReason; }
    public String code() { return code; }
    public boolean isIndividual() { return isIndividual; }
    public boolean isAvailable() { return isAvailable; }
    public boolean isVerified() { return isVerified; }
    public boolean isActive() { return isActive; }
    public String type() { return type; }
    public String role() { return role; }
    public Set<String> qualifications() { return qualifications; }
    public Set<String> paymentMethods() { return paymentMethods; }
    public Set<UUID> addresses() { return addresses; }
    public String biography() { return biography; }
    public Instant deletedAt() { return deletedAt; }
    public String name() { return name; }
    public String businessId() { return businessId; }
    public String niu() { return niu; }
    public String tradeRegistryNumber() { return tradeRegistryNumber; }
    public String website() { return website; }
    public String contactPhone() { return contactPhone; }
    public String privateAddress() { return privateAddress; }
    public String businessAddress() { return businessAddress; }
    public String businessProfile() { return businessProfile; }

    private static UUID requireUuid(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeCode(String code, String businessId, String name, UUID actorId) {
        String candidate = firstNonBlank(code, businessId, name, actorId == null ? null : actorId.toString());
        if (candidate == null) {
            throw new IllegalArgumentException("code is required");
        }
        return candidate.trim().replaceAll("[^A-Za-z0-9]+", "_").toUpperCase(Locale.ROOT);
    }

    private static String normalizeType(String value) {
        String normalized = normalize(value);
        return normalized == null ? "BUSINESS_ACTOR" : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeRole(String value) {
        String normalized = normalize(value);
        return normalized == null ? "OWNER" : normalized.toUpperCase(Locale.ROOT);
    }

    private static Set<String> normalizeStringSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT)).forEach(normalized::add);
        return Set.copyOf(normalized);
    }

    private static Set<UUID> normalizeUuidSet(Set<UUID> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<UUID> normalized = new LinkedHashSet<>();
        values.stream().filter(Objects::nonNull).forEach(normalized::add);
        return Set.copyOf(normalized);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
