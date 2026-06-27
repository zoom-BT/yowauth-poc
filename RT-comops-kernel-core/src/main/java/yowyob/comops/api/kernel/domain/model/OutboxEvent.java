package yowyob.comops.api.kernel.domain.model;

import yowyob.comops.api.common.domain.model.BaseEntity;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class OutboxEvent extends BaseEntity {

    private static final int MAX_ERROR_LENGTH = 1500;

    private final UUID organizationId;
    private final String eventType;
    private final String aggregateType;
    private final UUID aggregateId;
    private final Instant occurredAt;
    private final Map<String, Object> payload;
    private final OutboxEventStatus status;
    private final int attemptCount;
    private final Instant lastAttemptAt;
    private final Instant nextAttemptAt;
    private final String lastError;
    private final Instant deadLetteredAt;
    private final Instant publishedAt;
    private final UUID actorUserId;
    private final String clientApplicationId;
    private final String requestId;

    private OutboxEvent(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt, UUID organizationId,
            String eventType, String aggregateType, UUID aggregateId, Instant occurredAt,
            Map<String, Object> payload, OutboxEventStatus status, int attemptCount, Instant lastAttemptAt,
            Instant nextAttemptAt, String lastError, Instant deadLetteredAt, Instant publishedAt,
            UUID actorUserId, String clientApplicationId, String requestId) {
        super(id, tenantId, createdAt, updatedAt);
        this.organizationId = organizationId;
        this.eventType = requireText(eventType, "eventType");
        this.aggregateType = requireText(aggregateType, "aggregateType");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
        this.status = Objects.requireNonNull(status, "status is required");
        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount must be greater than or equal to zero");
        }
        this.attemptCount = attemptCount;
        this.lastAttemptAt = lastAttemptAt;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = normalizeError(lastError);
        this.deadLetteredAt = deadLetteredAt;
        this.publishedAt = publishedAt;
        this.actorUserId = actorUserId;
        this.clientApplicationId = trimToNull(clientApplicationId);
        this.requestId = trimToNull(requestId);
        validateState();
    }

    public static OutboxEvent create(BusinessEvent event) {
        return create(event, null, null, null);
    }

    public static OutboxEvent create(BusinessEvent event, UUID actorUserId, String clientApplicationId,
            String requestId) {
        Objects.requireNonNull(event, "event is required");
        Instant now = Instant.now();
        return new OutboxEvent(UUID.randomUUID(), event.tenantId(), now, now, event.organizationId(),
                event.eventType(), event.aggregateType(), event.aggregateId(), event.occurredAt(),
                event.payload(), OutboxEventStatus.PENDING, 0, null, now, null, null, null,
                actorUserId, clientApplicationId, requestId);
    }

    public static OutboxEvent rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID organizationId, String eventType, String aggregateType, UUID aggregateId, Instant occurredAt,
            Map<String, Object> payload, OutboxEventStatus status, int attemptCount, Instant lastAttemptAt,
            Instant nextAttemptAt, String lastError, Instant deadLetteredAt, Instant publishedAt) {
        return rehydrate(id, tenantId, createdAt, updatedAt, organizationId, eventType, aggregateType, aggregateId,
                occurredAt, payload, status, attemptCount, lastAttemptAt, nextAttemptAt, lastError, deadLetteredAt,
                publishedAt, null, null, null);
    }

    public static OutboxEvent rehydrate(UUID id, UUID tenantId, Instant createdAt, Instant updatedAt,
            UUID organizationId, String eventType, String aggregateType, UUID aggregateId, Instant occurredAt,
            Map<String, Object> payload, OutboxEventStatus status, int attemptCount, Instant lastAttemptAt,
            Instant nextAttemptAt, String lastError, Instant deadLetteredAt, Instant publishedAt,
            UUID actorUserId, String clientApplicationId, String requestId) {
        return new OutboxEvent(id, tenantId, createdAt, updatedAt, organizationId, eventType, aggregateType,
                aggregateId, occurredAt, payload, status, attemptCount, lastAttemptAt, nextAttemptAt, lastError,
                deadLetteredAt, publishedAt, actorUserId, clientApplicationId, requestId);
    }

    public UUID organizationId() { return organizationId; }
    public String eventType() { return eventType; }
    public String aggregateType() { return aggregateType; }
    public UUID aggregateId() { return aggregateId; }
    public Instant occurredAt() { return occurredAt; }
    public Map<String, Object> payload() { return payload; }
    public OutboxEventStatus status() { return status; }
    public int attemptCount() { return attemptCount; }
    public Instant lastAttemptAt() { return lastAttemptAt; }
    public Instant nextAttemptAt() { return nextAttemptAt; }
    public String lastError() { return lastError; }
    public Instant deadLetteredAt() { return deadLetteredAt; }
    public Instant publishedAt() { return publishedAt; }
    public UUID actorUserId() { return actorUserId; }
    public String clientApplicationId() { return clientApplicationId; }
    public String requestId() { return requestId; }

    public boolean isReadyForRelay(Instant asOf) {
        Instant effectiveAsOf = Objects.requireNonNull(asOf, "asOf is required");
        return status == OutboxEventStatus.PENDING && (nextAttemptAt == null || !nextAttemptAt.isAfter(effectiveAsOf));
    }

    public OutboxEvent markPublished(Instant publishedAt) {
        Instant effectivePublishedAt = Objects.requireNonNull(publishedAt, "publishedAt is required");
        return new OutboxEvent(id(), tenantId(), createdAt(), Instant.now(), organizationId, eventType,
                aggregateType, aggregateId, occurredAt, payload, OutboxEventStatus.PUBLISHED, attemptCount + 1,
                effectivePublishedAt, null, null, null, effectivePublishedAt,
                actorUserId, clientApplicationId, requestId);
    }

    public OutboxEvent scheduleRetry(Instant attemptedAt, Instant nextAttemptAt, String errorMessage) {
        Instant effectiveAttemptedAt = Objects.requireNonNull(attemptedAt, "attemptedAt is required");
        Instant effectiveNextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt is required");
        return new OutboxEvent(id(), tenantId(), createdAt(), Instant.now(), organizationId, eventType,
                aggregateType, aggregateId, occurredAt, payload, OutboxEventStatus.PENDING, attemptCount + 1,
                effectiveAttemptedAt, effectiveNextAttemptAt, errorMessage, null, null,
                actorUserId, clientApplicationId, requestId);
    }

    public OutboxEvent markDeadLetter(Instant attemptedAt, String errorMessage) {
        Instant effectiveAttemptedAt = Objects.requireNonNull(attemptedAt, "attemptedAt is required");
        return new OutboxEvent(id(), tenantId(), createdAt(), Instant.now(), organizationId, eventType,
                aggregateType, aggregateId, occurredAt, payload, OutboxEventStatus.DEAD_LETTER, attemptCount + 1,
                effectiveAttemptedAt, null, errorMessage, effectiveAttemptedAt, null,
                actorUserId, clientApplicationId, requestId);
    }

    private void validateState() {
        if (status == OutboxEventStatus.PUBLISHED) {
            if (publishedAt == null) {
                throw new IllegalArgumentException("published event must define publishedAt");
            }
            if (deadLetteredAt != null) {
                throw new IllegalArgumentException("published event cannot be dead-lettered");
            }
        }
        if (status == OutboxEventStatus.DEAD_LETTER) {
            if (deadLetteredAt == null) {
                throw new IllegalArgumentException("dead-letter event must define deadLetteredAt");
            }
            if (publishedAt != null) {
                throw new IllegalArgumentException("dead-letter event cannot be published");
            }
        }
        if (status == OutboxEventStatus.PENDING && publishedAt != null) {
            throw new IllegalArgumentException("pending event cannot define publishedAt");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeError(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > MAX_ERROR_LENGTH ? normalized.substring(0, MAX_ERROR_LENGTH) : normalized;
    }
}
