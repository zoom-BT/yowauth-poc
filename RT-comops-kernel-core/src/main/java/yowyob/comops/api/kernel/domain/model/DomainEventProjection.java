package yowyob.comops.api.kernel.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DomainEventProjection extends BaseEntity {

    private final UUID sourceEventId;
    private final UUID organizationId;
    private final String domainType;
    private final String eventType;
    private final String aggregateType;
    private final UUID aggregateId;
    private final String businessKey;
    private final String lifecycleStatus;
    private final Instant occurredAt;
    private final Map<String, Object> payload;

    private DomainEventProjection(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID sourceEventId,
            UUID organizationId, String domainType, String eventType, String aggregateType, UUID aggregateId,
            String businessKey, String lifecycleStatus, Instant occurredAt, Map<String, Object> payload) {
        super(id, tenantId, createdAt, updatedAt);
        this.sourceEventId = Objects.requireNonNull(sourceEventId, "sourceEventId is required");
        this.organizationId = organizationId;
        this.domainType = requireText(domainType, "domainType");
        this.eventType = requireText(eventType, "eventType");
        this.aggregateType = requireText(aggregateType, "aggregateType");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId is required");
        this.businessKey = normalizeNullableText(businessKey);
        this.lifecycleStatus = normalizeNullableText(lifecycleStatus);
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    public static DomainEventProjection create(OutboxEvent event, String domainType, String businessKey,
            String lifecycleStatus) {
        Objects.requireNonNull(event, "event is required");
        Instant now = Instant.now();
        return new DomainEventProjection(UUID.randomUUID(), event.tenantId(), now, now, event.id(),
                event.organizationId(),
                domainType, event.eventType(), event.aggregateType(), event.aggregateId(), businessKey,
                lifecycleStatus, event.occurredAt(), event.payload());
    }

    public static DomainEventProjection rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID sourceEventId, UUID organizationId, String domainType, String eventType, String aggregateType,
            UUID aggregateId, String businessKey, String lifecycleStatus, Instant occurredAt,
            Map<String, Object> payload) {
        return new DomainEventProjection(id, tenantId, createdAt, updatedAt, sourceEventId, organizationId,
                domainType, eventType, aggregateType, aggregateId, businessKey, lifecycleStatus, occurredAt,
                payload);
    }

    public UUID sourceEventId() { return sourceEventId; }
    public UUID organizationId() { return organizationId; }
    public String domainType() { return domainType; }
    public String eventType() { return eventType; }
    public String aggregateType() { return aggregateType; }
    public UUID aggregateId() { return aggregateId; }
    public String businessKey() { return businessKey; }
    public String lifecycleStatus() { return lifecycleStatus; }
    public Instant occurredAt() { return occurredAt; }
    public Map<String, Object> payload() { return payload; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase();
    }

    private static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
