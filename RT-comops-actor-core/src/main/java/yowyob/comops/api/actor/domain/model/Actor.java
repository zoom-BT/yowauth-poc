package yowyob.comops.api.actor.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Actor extends BaseEntity {

    private final UUID organizationId;
    private final String firstName;
    private final String lastName;
    private final String name;
    private final String phoneNumber;
    private final String email;
    private final String description;
    private final String type;
    private final String gender;
    private final String photoUri;
    private final UUID photoId;
    private final String nationality;
    private final LocalDate birthDate;
    private final String profession;
    private final String biography;
    private final Set<UUID> addresses;
    private final Set<UUID> contacts;
    private final Instant deletedAt;

    private Actor(
            UUID id,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt,
            UUID organizationId,
            String firstName,
            String lastName,
            String name,
            String phoneNumber,
            String email,
            String description,
            String type,
            String gender,
            String photoUri,
            UUID photoId,
            String nationality,
            LocalDate birthDate,
            String profession,
            String biography,
            Set<UUID> addresses,
            Set<UUID> contacts,
            Instant deletedAt) {
        super(id, tenantId, createdAt, updatedAt);
        this.organizationId = organizationId;
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.name = normalizeName(name, this.firstName, this.lastName);
        this.phoneNumber = normalize(phoneNumber);
        this.email = normalizeEmail(email);
        this.description = normalize(description);
        this.type = normalizeType(type);
        this.gender = normalize(gender);
        this.photoUri = normalize(photoUri);
        this.photoId = photoId;
        this.nationality = normalize(nationality);
        this.birthDate = birthDate;
        this.profession = normalize(profession);
        this.biography = normalize(biography);
        this.addresses = normalizeUuidSet(addresses);
        this.contacts = normalizeUuidSet(contacts);
        this.deletedAt = deletedAt;
    }

    public static Actor create(
            UUID tenantId,
            String firstName,
            String lastName,
            String phoneNumber,
            String email,
            String gender,
            String nationality,
            LocalDate birthDate,
            String profession,
            String biography) {
        return create(tenantId, null, firstName, lastName, null, phoneNumber, email, null, null, null, null, null,
                nationality, birthDate, profession, biography, Set.of(), Set.of());
    }

    public static Actor create(
            UUID tenantId,
            UUID organizationId,
            String firstName,
            String lastName,
            String name,
            String phoneNumber,
            String email,
            String description,
            String type,
            String gender,
            String photoUri,
            UUID photoId,
            String nationality,
            LocalDate birthDate,
            String profession,
            String biography,
            Set<UUID> addresses,
            Set<UUID> contacts) {
        Instant now = Instant.now();
        return new Actor(UUID.randomUUID(), tenantId, now, now, organizationId, firstName, lastName, name,
                phoneNumber, email, description, type, gender, photoUri, photoId, nationality, birthDate,
                profession, biography, addresses, contacts, null);
    }

    public static Actor rehydrate(
            UUID id,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt,
            String firstName,
            String lastName,
            String phoneNumber,
            String email,
            String gender,
            String nationality,
            LocalDate birthDate,
            String profession,
            String biography,
            Instant deletedAt) {
        return rehydrate(id, tenantId, createdAt, updatedAt, null, firstName, lastName, null, phoneNumber, email,
                null, null, gender, null, null, nationality, birthDate, profession, biography, Set.of(), Set.of(),
                deletedAt);
    }

    public static Actor rehydrate(
            UUID id,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt,
            UUID organizationId,
            String firstName,
            String lastName,
            String name,
            String phoneNumber,
            String email,
            String description,
            String type,
            String gender,
            String photoUri,
            UUID photoId,
            String nationality,
            LocalDate birthDate,
            String profession,
            String biography,
            Set<UUID> addresses,
            Set<UUID> contacts,
            Instant deletedAt) {
        return new Actor(id, tenantId, createdAt, updatedAt, organizationId, firstName, lastName, name, phoneNumber,
                email, description, type, gender, photoUri, photoId, nationality, birthDate, profession, biography,
                addresses, contacts, deletedAt);
    }

    public UUID organizationId() {
        return organizationId;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String name() {
        return name;
    }

    public String displayName() {
        return name;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String email() {
        return email;
    }

    public String description() {
        return description;
    }

    public String type() {
        return type;
    }

    public String gender() {
        return gender;
    }

    public String photoUri() {
        return photoUri;
    }

    public UUID photoId() {
        return photoId;
    }

    public String nationality() {
        return nationality;
    }

    public LocalDate birthDate() {
        return birthDate;
    }

    public String profession() {
        return profession;
    }

    public String biography() {
        return biography;
    }

    public Set<UUID> addresses() {
        return addresses;
    }

    public Set<UUID> contacts() {
        return contacts;
    }

    public Instant deletedAt() {
        return deletedAt;
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

    private static String normalizeEmail(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeType(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeName(String explicitName, String firstName, String lastName) {
        String normalized = normalize(explicitName);
        return normalized != null ? normalized : firstName.trim() + " " + lastName.trim();
    }

    private static Set<UUID> normalizeUuidSet(Set<UUID> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<UUID> normalized = new LinkedHashSet<>();
        values.stream().filter(Objects::nonNull).forEach(normalized::add);
        return Set.copyOf(normalized);
    }
}
