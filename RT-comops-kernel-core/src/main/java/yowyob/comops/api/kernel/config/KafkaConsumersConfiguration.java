package yowyob.comops.api.kernel.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "iwm.outbox.consumers", name = "mode", havingValue = "kafka")
public class KafkaConsumersConfiguration {

    @Bean(name = "iwmKafkaConsumerTopics")
    String[] iwmKafkaConsumerTopics(OutboxConsumersProperties consumersProperties) {
        return consumersProperties.getKafka().getTopics().toArray(String[]::new);
    }

    @Bean
    CommonErrorHandler iwmKafkaConsumerErrorHandler(KafkaTemplate<String, String> kafkaTemplate,
            KafkaOutboxDeliveryProperties deliveryProperties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new org.apache.kafka.common.TopicPartition(
                        deliveryProperties.getDeadLetterTopic(), record.partition()));
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0d);
        backOff.setMaxInterval(5000L);
        backOff.setMaxElapsedTime(15000L);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean(name = "iwmKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> iwmKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            CommonErrorHandler commonErrorHandler,
            OutboxConsumersProperties consumersProperties) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler);
        factory.setConcurrency(consumersProperties.getKafka().getConcurrency());
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setAsyncAcks(true);
        factory.getContainerProperties().getKafkaConsumerProperties().put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return factory;
    }
}
