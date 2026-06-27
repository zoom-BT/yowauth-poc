package yowyob.comops.api.kernel.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.outbox.relay")
public class OutboxRelayProperties {

    private boolean enabled;
    private int batchSize = 100;
    private int deliveryConcurrency = 4;
    private Duration fixedDelay = Duration.ofSeconds(5);
    private int maxAttempts = 5;
    private Duration initialBackoff = Duration.ofSeconds(5);
    private double backoffMultiplier = 2.0d;
    private Duration maxBackoff = Duration.ofMinutes(5);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(Duration fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    public int getDeliveryConcurrency() {
        return deliveryConcurrency;
    }

    public void setDeliveryConcurrency(int deliveryConcurrency) {
        this.deliveryConcurrency = deliveryConcurrency;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(Duration maxBackoff) {
        this.maxBackoff = maxBackoff;
    }
}
