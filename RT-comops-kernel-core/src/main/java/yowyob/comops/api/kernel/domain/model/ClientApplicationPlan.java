package yowyob.comops.api.kernel.domain.model;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class ClientApplicationPlan {
    private final UUID id;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String code;
    private final String displayName;
    private final String description;
    private final List<String> allowedServices;
    private final boolean systemDefault;

    private ClientApplicationPlan(UUID id, Instant createdAt, Instant updatedAt, String code, String displayName,
            String description, List<String> allowedServices, boolean systemDefault) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.code = normalizeCode(code);
        this.displayName = requireText(displayName, "displayName");
        this.description = normalizeNullableText(description);
        this.allowedServices = normalizeServices(allowedServices);
        this.systemDefault = systemDefault;
    }

    public static ClientApplicationPlan create(String code, String displayName, String description, List<String> allowedServices) {
        Instant now = Instant.now();
        return new ClientApplicationPlan(UUID.randomUUID(), now, now, code, displayName, description, allowedServices, false);
    }

    public static ClientApplicationPlan system(String code, String displayName, String description, List<String> allowedServices) {
        Instant now = Instant.EPOCH;
        return new ClientApplicationPlan(UUID.nameUUIDFromBytes(("client-application-plan:" + normalizeCode(code)).getBytes()),
                now, now, code, displayName, description, allowedServices, true);
    }

    public static ClientApplicationPlan rehydrate(UUID id, Instant createdAt, Instant updatedAt, String code,
            String displayName, String description, List<String> allowedServices, boolean systemDefault) {
        return new ClientApplicationPlan(id, createdAt, updatedAt, code, displayName, description, allowedServices, systemDefault);
    }

    public ClientApplicationPlan update(String displayName, String description, List<String> allowedServices) {
        if (systemDefault) {
            throw new IllegalArgumentException("default client application plans cannot be modified");
        }
        return new ClientApplicationPlan(id, createdAt, Instant.now(), code, displayName, description, allowedServices, false);
    }

    public UUID id() { return id; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public String code() { return code; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    public List<String> allowedServices() { return allowedServices; }
    public boolean systemDefault() { return systemDefault; }

    public static String normalizeCode(String value) {
        return requireText(value, "code").replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private static List<String> normalizeServices(List<String> services) {
        if (services == null || services.isEmpty()) {
            throw new IllegalArgumentException("allowedServices must not be empty");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        // Lenient : accepte les codes natifs (enum) ET les services externes enregistrés (registre DB).
        // La validation "code réellement connu" est faite en amont par ServiceCatalog (couche service).
        services.stream().map(PlatformServiceCode::resolveCanonical).forEach(normalized::add);
        return PlatformServiceCode.orderCodes(normalized);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalizeNullableText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
