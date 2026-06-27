package yowyob.comops.api.kernel.domain.model;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ClientApplication {

    private final UUID id;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String clientId;
    private final String name;
    private final String description;
    private final String secretHash;
    private final ClientApplicationStatus status;
    private final boolean systemManaged;
    private final Set<String> allowedServiceCodes;
    private final Instant lastAuthenticatedAt;
    private final Instant secretRotatedAt;

    private ClientApplication(
            UUID id,
            Instant createdAt,
            Instant updatedAt,
            String clientId,
            String name,
            String description,
            String secretHash,
            ClientApplicationStatus status,
            boolean systemManaged,
            Set<String> allowedServiceCodes,
            Instant lastAuthenticatedAt,
            Instant secretRotatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.clientId = normalizeClientId(clientId);
        this.name = requireText(name, "name");
        this.description = normalizeNullableText(description);
        this.secretHash = requireText(secretHash, "secretHash");
        this.status = Objects.requireNonNull(status, "status is required");
        this.systemManaged = systemManaged;
        this.allowedServiceCodes = normalizeAllowedServiceCodes(allowedServiceCodes);
        this.lastAuthenticatedAt = lastAuthenticatedAt;
        this.secretRotatedAt = secretRotatedAt == null ? updatedAt : secretRotatedAt;
    }

    public static ClientApplication register(
            String clientId,
            String name,
            String description,
            String secretHash,
            Set<String> allowedServiceCodes,
            boolean systemManaged) {
        Instant now = Instant.now();
        return new ClientApplication(
                UUID.randomUUID(),
                now,
                now,
                clientId,
                name,
                description,
                secretHash,
                ClientApplicationStatus.ACTIVE,
                systemManaged,
                allowedServiceCodes,
                null,
                now);
    }

    public static ClientApplication rehydrate(
            UUID id,
            Instant createdAt,
            Instant updatedAt,
            String clientId,
            String name,
            String description,
            String secretHash,
            ClientApplicationStatus status,
            boolean systemManaged,
            Set<String> allowedServiceCodes,
            Instant lastAuthenticatedAt,
            Instant secretRotatedAt) {
        return new ClientApplication(id, createdAt, updatedAt, clientId, name, description, secretHash, status,
                systemManaged, allowedServiceCodes, lastAuthenticatedAt, secretRotatedAt);
    }

    public UUID id() {
        return id;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public String clientId() {
        return clientId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String secretHash() {
        return secretHash;
    }

    public ClientApplicationStatus status() {
        return status;
    }

    public boolean systemManaged() {
        return systemManaged;
    }

    public Set<String> allowedServiceCodes() {
        return allowedServiceCodes;
    }

    public Instant lastAuthenticatedAt() {
        return lastAuthenticatedAt;
    }

    public Instant secretRotatedAt() {
        return secretRotatedAt;
    }

    public boolean isActive() {
        return status == ClientApplicationStatus.ACTIVE;
    }

    public boolean canAccessService(String serviceCode) {
        return allowedServiceCodes.contains(PlatformServiceCode.resolveCanonical(serviceCode));
    }

    public ClientApplication updateDefinition(String name, String description, Set<String> allowedServiceCodes,
            boolean systemManaged) {
        return new ClientApplication(id, createdAt, Instant.now(), clientId, name, description, secretHash, status,
                systemManaged, allowedServiceCodes, lastAuthenticatedAt, secretRotatedAt);
    }

    public ClientApplication rotateSecret(String secretHash) {
        Instant now = Instant.now();
        return new ClientApplication(id, createdAt, now, clientId, name, description, secretHash,
                ClientApplicationStatus.ACTIVE, systemManaged, allowedServiceCodes, lastAuthenticatedAt, now);
    }

    public ClientApplication markAuthenticated() {
        Instant now = Instant.now();
        return new ClientApplication(id, createdAt, now, clientId, name, description, secretHash, status,
                systemManaged, allowedServiceCodes, now, secretRotatedAt);
    }

    public ClientApplication revoke() {
        return new ClientApplication(id, createdAt, Instant.now(), clientId, name, description, secretHash,
                ClientApplicationStatus.REVOKED, systemManaged, allowedServiceCodes, lastAuthenticatedAt,
                secretRotatedAt);
    }

    public ClientApplication activate() {
        return new ClientApplication(id, createdAt, Instant.now(), clientId, name, description, secretHash,
                ClientApplicationStatus.ACTIVE, systemManaged, allowedServiceCodes, lastAuthenticatedAt,
                secretRotatedAt);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalizeClientId(String clientId) {
        return requireText(clientId, "clientId").toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Set<String> normalizeAllowedServiceCodes(Set<String> allowedServiceCodes) {
        Set<String> requestedServices = allowedServiceCodes == null || allowedServiceCodes.isEmpty()
                ? new LinkedHashSet<>(PlatformServiceCode.catalog().stream().map(PlatformServiceCode::code).toList())
                : new LinkedHashSet<>(allowedServiceCodes);
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        // Lenient : codes natifs (enum) ET services externes enregistrés. La validation stricte
        // (code réellement connu du catalogue enum∪registre) est faite par ServiceCatalog en amont.
        requestedServices.stream()
                .map(PlatformServiceCode::resolveCanonical)
                .forEach(normalized::add);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("allowedServiceCodes must not be empty");
        }
        return Set.copyOf(normalized);
    }
}
