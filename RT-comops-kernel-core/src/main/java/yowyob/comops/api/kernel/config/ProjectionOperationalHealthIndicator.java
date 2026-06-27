package yowyob.comops.api.kernel.config;

import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.application.port.out.OutboxEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("projectionRuntimeHealthIndicator")
public class ProjectionOperationalHealthIndicator implements ReactiveHealthIndicator {

    private final DomainEventProjectionRepository domainEventProjectionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final KernelObservabilityProperties properties;

    public ProjectionOperationalHealthIndicator(DomainEventProjectionRepository domainEventProjectionRepository,
            OutboxEventRepository outboxEventRepository,
            KernelObservabilityProperties properties) {
        this.domainEventProjectionRepository = domainEventProjectionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.properties = properties;
    }

    @Override
    public Mono<Health> health() {
        return Mono.zip(
                        domainEventProjectionRepository.countAll(),
                        domainEventProjectionRepository.findLatestCreatedAt().defaultIfEmpty(Instant.EPOCH),
                        outboxEventRepository.findLatestPublishedAt().defaultIfEmpty(Instant.EPOCH))
                .map(tuple -> buildHealth(tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }

    private Health buildHealth(long totalProjections, Instant latestProjectionCreatedAt, Instant latestPublishedAt) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("totalProjections", totalProjections);
        if (!Instant.EPOCH.equals(latestPublishedAt)) {
            details.put("latestPublishedAt", latestPublishedAt);
        }
        if (Instant.EPOCH.equals(latestPublishedAt)) {
            return Health.up().withDetails(details).build();
        }
        if (Instant.EPOCH.equals(latestProjectionCreatedAt)) {
            return Health.down()
                    .withDetails(details)
                    .withDetail("reason", "Published outbox events exist but no projection has been materialized.")
                    .build();
        }
        details.put("latestProjectionCreatedAt", latestProjectionCreatedAt);
        if (latestProjectionCreatedAt.isBefore(latestPublishedAt)) {
            Duration projectionLag = Duration.between(latestProjectionCreatedAt, latestPublishedAt);
            details.put("projectionLagSeconds", projectionLag.toSeconds());
            if (projectionLag.compareTo(properties.getProjections().getMaxSilence()) > 0) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("reason", "Projection pipeline appears stale.")
                        .build();
            }
        }
        return Health.up().withDetails(details).build();
    }
}
