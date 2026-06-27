package yowyob.comops.api.kernel.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record BusinessEvent(
        UUID tenantId,
        UUID organizationId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        Map<String, Object> payload) {

    public BusinessEvent {
        Objects.requireNonNull(tenantId, "tenantId is required");
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType is required");
        }
        Objects.requireNonNull(aggregateId, "aggregateId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    public static BusinessEvent now(UUID tenantId, UUID organizationId, String eventType,
            String aggregateType, UUID aggregateId, Map<String, Object> payload) {
        return new BusinessEvent(tenantId, organizationId, eventType, aggregateType, aggregateId,
                Instant.now(), orderedPayload(payload));
    }

    private static Map<String, Object> orderedPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(payload);
    }
}
