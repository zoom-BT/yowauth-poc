package yowyob.comops.api.kernel.config;

import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.application.port.out.OutboxEventRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEventStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KernelMetricsRefresher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KernelMetricsRefresher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final DomainEventProjectionRepository domainEventProjectionRepository;
    private final KernelObservabilityProperties properties;
    private final AtomicLong outboxPending = new AtomicLong();
    private final AtomicLong outboxPublished = new AtomicLong();
    private final AtomicLong outboxDeadLetter = new AtomicLong();
    private final AtomicLong projectionsTotal = new AtomicLong();
    private final AtomicLong oldestPendingAgeSeconds = new AtomicLong();
    private final AtomicLong latestProjectionAgeSeconds = new AtomicLong();
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    public KernelMetricsRefresher(OutboxEventRepository outboxEventRepository,
            DomainEventProjectionRepository domainEventProjectionRepository,
            KernelObservabilityProperties properties,
            MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.domainEventProjectionRepository = domainEventProjectionRepository;
        this.properties = properties;
        Gauge.builder("iwm.outbox.pending", outboxPending, AtomicLong::get).register(meterRegistry);
        Gauge.builder("iwm.outbox.published", outboxPublished, AtomicLong::get).register(meterRegistry);
        Gauge.builder("iwm.outbox.dead_letter", outboxDeadLetter, AtomicLong::get).register(meterRegistry);
        Gauge.builder("iwm.outbox.oldest_pending_age.seconds", oldestPendingAgeSeconds, AtomicLong::get)
                .register(meterRegistry);
        Gauge.builder("iwm.projections.total", projectionsTotal, AtomicLong::get).register(meterRegistry);
        Gauge.builder("iwm.projections.latest_age.seconds", latestProjectionAgeSeconds, AtomicLong::get)
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${iwm.observability.metrics.refresh-interval:PT30S}")
    public void refresh() {
        if (!refreshInProgress.compareAndSet(false, true)) {
            LOGGER.debug("Skipping kernel metrics refresh because a previous refresh is still running.");
            return;
        }
        Instant now = Instant.now();
        Mono.zip(
                        outboxEventRepository.countByStatus(OutboxEventStatus.PENDING),
                        outboxEventRepository.countByStatus(OutboxEventStatus.PUBLISHED),
                        outboxEventRepository.countByStatus(OutboxEventStatus.DEAD_LETTER),
                        outboxEventRepository.findOldestPendingOccurredAt().defaultIfEmpty(Instant.EPOCH),
                        domainEventProjectionRepository.countAll(),
                        domainEventProjectionRepository.findLatestOccurredAt().defaultIfEmpty(Instant.EPOCH))
                .doOnNext(tuple -> {
                    outboxPending.set(tuple.getT1());
                    outboxPublished.set(tuple.getT2());
                    outboxDeadLetter.set(tuple.getT3());
                    outboxOldestPending(tuple.getT4(), now);
                    projectionsTotal.set(tuple.getT5());
                    latestProjectionAge(tuple.getT6(), now);
                })
                .doOnError(error -> LOGGER.warn("kernel metrics refresh failed", error))
                .onErrorResume(error -> Mono.empty())
                .doFinally(signal -> refreshInProgress.set(false))
                .subscribe();
    }

    private void outboxOldestPending(Instant oldestPendingOccurredAt, Instant now) {
        if (Instant.EPOCH.equals(oldestPendingOccurredAt)) {
            oldestPendingAgeSeconds.set(0L);
            return;
        }
        oldestPendingAgeSeconds.set(Math.max(0L, Duration.between(oldestPendingOccurredAt, now).toSeconds()));
    }

    private void latestProjectionAge(Instant latestProjectionOccurredAt, Instant now) {
        if (Instant.EPOCH.equals(latestProjectionOccurredAt)) {
            latestProjectionAgeSeconds.set(0L);
            return;
        }
        latestProjectionAgeSeconds.set(Math.max(0L, Duration.between(latestProjectionOccurredAt, now).toSeconds()));
    }
}
