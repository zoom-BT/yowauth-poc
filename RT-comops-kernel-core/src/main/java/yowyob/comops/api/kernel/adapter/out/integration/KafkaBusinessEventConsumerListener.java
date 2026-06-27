package yowyob.comops.api.kernel.adapter.out.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.kernel.application.port.out.BusinessEventConsumer;
import yowyob.comops.api.kernel.config.KafkaOutboxDeliveryProperties;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import yowyob.comops.api.kernel.domain.model.OutboxEventStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.support.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "iwm.outbox.consumers", name = "mode", havingValue = "kafka")
public class KafkaBusinessEventConsumerListener {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() { };
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaBusinessEventConsumerListener.class);

    private final List<BusinessEventConsumer> consumers;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaOutboxDeliveryProperties deliveryProperties;

    public KafkaBusinessEventConsumerListener(List<BusinessEventConsumer> consumers,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaOutboxDeliveryProperties deliveryProperties) {
        this.consumers = consumers;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.deliveryProperties = deliveryProperties;
    }

    @KafkaListener(
            topics = "#{@iwmKafkaConsumerTopics}",
            groupId = "${iwm.outbox.consumers.kafka.group-id:iwm-outbox-consumers}",
            containerFactory = "iwmKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        OutboxEvent event;
        try {
            event = deserialize(record.value());
        } catch (RuntimeException exception) {
            LOGGER.error("Kafka business event deserialization failed for topic={} offset={}",
                    record.topic(), record.offset(), exception);
            publishToDeadLetter(record, exception)
                    .doOnSuccess(result -> acknowledgment.acknowledge())
                    .subscribe();
            return;
        }
        List<BusinessEventConsumer> matchingConsumers = consumers.stream()
                .filter(consumer -> consumer.supports(event))
                .toList();
        if (matchingConsumers.isEmpty()) {
            LOGGER.warn("No business event consumer registered for eventType={} aggregateType={} topic={}",
                    event.eventType(), event.aggregateType(), record.topic());
            acknowledgment.acknowledge();
            return;
        }
        Flux.fromIterable(matchingConsumers)
                .concatMap(consumer -> consumer.consume(event))
                .then()
                .doOnSuccess(ignored -> acknowledgment.acknowledge())
                .doOnError(exception -> LOGGER.error(
                        "Kafka business event consumption failed for outboxId={} eventType={} aggregateType={} topic={}",
                        event.id(), event.eventType(), event.aggregateType(), record.topic(), exception))
                .onErrorResume(exception -> publishToDeadLetter(record, exception)
                        .doOnSuccess(result -> acknowledgment.acknowledge())
                        .then())
                .subscribe();
    }

    private OutboxEvent deserialize(String payload) {
        try {
            JsonOutboxEvent event = objectMapper.readValue(payload, JsonOutboxEvent.class);
            return OutboxEvent.rehydrate(event.id(), event.tenantId(), event.createdAt(), event.updatedAt(),
                    event.organizationId(), event.eventType(), event.aggregateType(), event.aggregateId(),
                    event.occurredAt(), objectMapper.convertValue(event.payload(), PAYLOAD_TYPE),
                    OutboxEventStatus.valueOf(event.status()), event.attemptCount(), event.lastAttemptAt(),
                    event.nextAttemptAt(), event.lastError(), event.deadLetteredAt(), event.publishedAt(),
                    event.actorUserId(), event.clientApplicationId(), event.requestId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize Kafka outbox event", exception);
        }
    }

    private Mono<?> publishToDeadLetter(ConsumerRecord<String, String> record, Throwable error) {
        return Mono.fromFuture(kafkaTemplate.send(MessageBuilder.withPayload(record.value())
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC,
                                deliveryProperties.getDeadLetterTopic())
                        .setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, record.key())
                        .setHeader("iwm-dead-letter-source-topic", record.topic())
                        .setHeader("iwm-dead-letter-source-offset", Long.toString(record.offset()))
                        .setHeader("iwm-dead-letter-error", truncateError(error))
                        .build()))
                .doOnError(deadLetterError -> LOGGER.error(
                        "Kafka dead-letter publication failed for sourceTopic={} offset={}",
                        record.topic(), record.offset(), deadLetterError));
    }

    private String truncateError(Throwable error) {
        String message = error == null ? "unknown error" : error.getMessage();
        if (message == null || message.isBlank()) {
            message = error == null ? "unknown error" : error.getClass().getSimpleName();
        }
        return message.length() > 512 ? message.substring(0, 512) : message;
    }

    private record JsonOutboxEvent(
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
