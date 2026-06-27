package yowyob.comops.api.kernel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.outbox.delivery.kafka")
public class KafkaOutboxDeliveryProperties {

    private String topicPrefix = "iwm.events";
    private boolean createPerAggregateTopic = true;
    private String deadLetterTopic = "iwm.events.dead-letter";

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public boolean isCreatePerAggregateTopic() {
        return createPerAggregateTopic;
    }

    public void setCreatePerAggregateTopic(boolean createPerAggregateTopic) {
        this.createPerAggregateTopic = createPerAggregateTopic;
    }

    public String getDeadLetterTopic() {
        return deadLetterTopic;
    }

    public void setDeadLetterTopic(String deadLetterTopic) {
        this.deadLetterTopic = deadLetterTopic;
    }
}
