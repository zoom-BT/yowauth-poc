package yowyob.comops.api.kernel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxRuntimeConfiguration {

    public OutboxRuntimeConfiguration(
            @Value("${iwm.outbox.delivery.type:kafka}") String deliveryType,
            OutboxConsumersProperties consumersProperties,
            OutboxRelayProperties relayProperties,
            OutboxRuntimeGuardProperties guardProperties) {
        validate(deliveryType, consumersProperties, relayProperties, guardProperties);
    }

    private void validate(
            String deliveryType,
            OutboxConsumersProperties consumersProperties,
            OutboxRelayProperties relayProperties,
            OutboxRuntimeGuardProperties guardProperties) {
        boolean kafkaDelivery = "kafka".equalsIgnoreCase(deliveryType);
        boolean kafkaConsumers = "kafka".equalsIgnoreCase(consumersProperties.getMode());
        if (kafkaDelivery && kafkaConsumers && !relayProperties.isEnabled() && !guardProperties.isAllowManualRelayOnly()) {
            throw new IllegalStateException(
                    "Invalid outbox runtime configuration: Kafka delivery and Kafka consumers require iwm.outbox.relay.enabled=true, unless manual relay only is explicitly allowed.");
        }
    }
}
