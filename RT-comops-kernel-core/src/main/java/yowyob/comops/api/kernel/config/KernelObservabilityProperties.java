package yowyob.comops.api.kernel.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.observability")
public class KernelObservabilityProperties {

    private final Outbox outbox = new Outbox();
    private final Projections projections = new Projections();
    private final Metrics metrics = new Metrics();

    public Outbox getOutbox() {
        return outbox;
    }

    public Projections getProjections() {
        return projections;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public static class Outbox {
        private long pendingWarningThreshold = 100L;
        private long deadLetterCriticalThreshold = 1L;
        private Duration maxPendingAge = Duration.ofMinutes(15);

        public long getPendingWarningThreshold() {
            return pendingWarningThreshold;
        }

        public void setPendingWarningThreshold(long pendingWarningThreshold) {
            this.pendingWarningThreshold = pendingWarningThreshold;
        }

        public long getDeadLetterCriticalThreshold() {
            return deadLetterCriticalThreshold;
        }

        public void setDeadLetterCriticalThreshold(long deadLetterCriticalThreshold) {
            this.deadLetterCriticalThreshold = deadLetterCriticalThreshold;
        }

        public Duration getMaxPendingAge() {
            return maxPendingAge;
        }

        public void setMaxPendingAge(Duration maxPendingAge) {
            this.maxPendingAge = maxPendingAge;
        }
    }

    public static class Projections {
        private Duration maxSilence = Duration.ofMinutes(30);

        public Duration getMaxSilence() {
            return maxSilence;
        }

        public void setMaxSilence(Duration maxSilence) {
            this.maxSilence = maxSilence;
        }
    }

    public static class Metrics {
        private Duration refreshInterval = Duration.ofSeconds(30);

        public Duration getRefreshInterval() {
            return refreshInterval;
        }

        public void setRefreshInterval(Duration refreshInterval) {
            this.refreshInterval = refreshInterval;
        }
    }
}
