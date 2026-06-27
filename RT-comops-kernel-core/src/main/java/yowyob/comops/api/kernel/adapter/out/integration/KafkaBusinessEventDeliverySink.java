package yowyob.comops.api.kernel.adapter.out.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.kernel.application.port.out.BusinessEventDeliverySink;
import yowyob.comops.api.kernel.config.KafkaOutboxDeliveryProperties;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Order(1)
@ConditionalOnProperty(prefix = "iwm.outbox.delivery", name = "type", havingValue = "kafka", matchIfMissing = true)
public class KafkaBusinessEventDeliverySink implements BusinessEventDeliverySink {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaOutboxDeliveryProperties properties;
    private final Counter successCounter;
    private final Counter failureCounter;

    public KafkaBusinessEventDeliverySink(KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            KafkaOutboxDeliveryProperties properties,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.successCounter = Counter.builder("iwm.kafka.outbox.delivery.success")
                .description("Successful Kafka deliveries from the outbox relay")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("iwm.kafka.outbox.delivery.failure")
                .description("Failed Kafka deliveries from the outbox relay")
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> deliver(OutboxEvent event) {
        return Mono.fromFuture(kafkaTemplate.send(buildMessage(event)))
                .doOnSuccess(result -> successCounter.increment())
                .doOnError(error -> failureCounter.increment())
                .then();
    }

    private Message<String> buildMessage(OutboxEvent event) {
        return MessageBuilder.withPayload(toJson(event))
                .setHeader(KafkaHeaders.TOPIC, resolveTopic(event))
                .setHeader(KafkaHeaders.KEY, event.aggregateId().toString())
                .setHeader("iwm-event-type", event.eventType())
                .setHeader("iwm-aggregate-type", event.aggregateType())
                .setHeader("iwm-aggregate-id", event.aggregateId().toString())
                .setHeader("iwm-tenant-id", event.tenantId().toString())
                .setHeader("iwm-organization-id", event.organizationId() == null ? "" : event.organizationId().toString())
                .setHeader("iwm-outbox-id", event.id().toString())
                .build();
    }

    private String resolveTopic(OutboxEvent event) {
        String base = properties.getTopicPrefix().trim();
        if (!properties.isCreatePerAggregateTopic()) {
            return base;
        }
        return base + "." + event.aggregateType().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String toJson(OutboxEvent event) {
        try {
            return objectMapper.writeValueAsString(new KafkaOutboxEventMessage(
                    event.id(),
                    event.tenantId(),
                    event.createdAt(),
                    event.updatedAt(),
                    event.organizationId(),
                    event.eventType(),
                    event.aggregateType(),
                    event.aggregateId(),
                    event.occurredAt(),
                    event.payload(),
                    event.status().name(),
                    event.attemptCount(),
                    event.lastAttemptAt(),
                    event.nextAttemptAt(),
                    event.lastError(),
                    event.deadLetteredAt(),
                    event.publishedAt(),
                    event.actorUserId(),
                    event.clientApplicationId(),
                    event.requestId()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize outbox event for Kafka delivery", exception);
        }
    }

    private record KafkaOutboxEventMessage(
            UUID id,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt,
            UUID organizationId,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            Instant occurredAt,
            Map<String, Object> payload,
            String status,
            int attemptCount,
            Instant lastAttemptAt,
            Instant nextAttemptAt,
            String lastError,
            Instant deadLetteredAt,
            Instant publishedAt,
            UUID actorUserId,
            String clientApplicationId,
            String requestId) {
    }
}
