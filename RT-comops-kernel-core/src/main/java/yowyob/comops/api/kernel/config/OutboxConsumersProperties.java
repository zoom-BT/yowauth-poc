package yowyob.comops.api.kernel.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.outbox.consumers")
public class OutboxConsumersProperties {

    private String mode = "kafka";
    private final Kafka kafka = new Kafka();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public static class Kafka {
        private String groupId = "iwm-outbox-consumers";
        private String topicPattern = "^iwm\\.events\\..+$";
        private List<String> topics = new ArrayList<>();
        private int concurrency = 1;

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getTopicPattern() {
            return topicPattern;
        }

        public void setTopicPattern(String topicPattern) {
            this.topicPattern = topicPattern;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics == null ? new ArrayList<>() : new ArrayList<>(topics);
        }

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }
    }
}
