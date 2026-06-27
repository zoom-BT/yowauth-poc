package yowyob.comops.api.kernel.config;

import yowyob.comops.api.kernel.application.port.out.OutboxEventRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEventStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("outboxRuntimeHealthIndicator")
public class OutboxOperationalHealthIndicator implements ReactiveHealthIndicator {

    private final OutboxEventRepository outboxEventRepository;
    private final KernelObservabilityProperties properties;

    public OutboxOperationalHealthIndicator(OutboxEventRepository outboxEventRepository,
            KernelObservabilityProperties properties) {
        this.outboxEventRepository = outboxEventRepository;
        this.properties = properties;
    }

    @Override
    public Mono<Health> health() {
        Instant now = Instant.now();
        return Mono.zip(
                        outboxEventRepository.countByStatus(OutboxEventStatus.PENDING),
                        outboxEventRepository.countByStatus(OutboxEventStatus.PUBLISHED),
                        outboxEventRepository.countByStatus(OutboxEventStatus.DEAD_LETTER),
                        outboxEventRepository.findOldestPendingOccurredAt().defaultIfEmpty(Instant.EPOCH))
                .map(tuple -> buildHealth(now, tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()));
    }

    private Health buildHealth(Instant now, long pendingCount, long publishedCount, long deadLetterCount,
            Instant oldestPendingOccurredAt) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("pendingCount", pendingCount);
        details.put("publishedCount", publishedCount);
        details.put("deadLetterCount", deadLetterCount);
        if (!Instant.EPOCH.equals(oldestPendingOccurredAt)) {
            Duration oldestPendingAge = Duration.between(oldestPendingOccurredAt, now);
            details.put("oldestPendingOccurredAt", oldestPendingOccurredAt);
            details.put("oldestPendingAgeSeconds", oldestPendingAge.toSeconds());
            if (oldestPendingAge.compareTo(properties.getOutbox().getMaxPendingAge()) > 0) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("reason", "Outbox pending age exceeded threshold.")
                        .build();
            }
        }
        if (deadLetterCount >= properties.getOutbox().getDeadLetterCriticalThreshold()
                && properties.getOutbox().getDeadLetterCriticalThreshold() > 0) {
            return Health.down()
                    .withDetails(details)
                    .withDetail("reason", "Dead-letter events present in outbox.")
                    .build();
        }
        if (pendingCount >= properties.getOutbox().getPendingWarningThreshold()
                && properties.getOutbox().getPendingWarningThreshold() > 0) {
            return Health.status("DEGRADED")
                    .withDetails(details)
                    .withDetail("reason", "Pending outbox backlog above threshold.")
                    .build();
        }
        return Health.up().withDetails(details).build();
    }
}
