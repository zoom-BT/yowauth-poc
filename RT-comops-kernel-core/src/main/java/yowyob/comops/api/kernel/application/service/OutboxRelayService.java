package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.kernel.application.port.in.RelayOutboxEventsUseCase;
import yowyob.comops.api.kernel.application.port.out.BusinessEventDeliverySink;
import yowyob.comops.api.kernel.application.port.out.OutboxEventRepository;
import yowyob.comops.api.kernel.config.OutboxRelayProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OutboxRelayService implements RelayOutboxEventsUseCase {

    private final OutboxEventRepository outboxEventRepository;
    private final List<BusinessEventDeliverySink> deliverySinks;
    private final OutboxRelayProperties relayProperties;
    private final MeterRegistry meterRegistry;
    private final Counter relayPublishedCounter;
    private final Counter relayFailedCounter;
    private final Timer relayBatchTimer;

    public OutboxRelayService(OutboxEventRepository outboxEventRepository,
            List<BusinessEventDeliverySink> deliverySinks,
            OutboxRelayProperties relayProperties,
            MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.deliverySinks = deliverySinks;
        this.relayProperties = relayProperties;
        this.meterRegistry = meterRegistry;
        this.relayPublishedCounter = Counter.builder("iwm.outbox.relay.events.published")
                .description("Number of outbox events successfully relayed")
                .register(meterRegistry);
        this.relayFailedCounter = Counter.builder("iwm.outbox.relay.events.failed")
                .description("Number of outbox relay attempts that failed")
                .register(meterRegistry);
        this.relayBatchTimer = Timer.builder("iwm.outbox.relay.batch.duration")
                .description("Duration of outbox relay batches")
                .register(meterRegistry);
    }

    @Override
    public Mono<Integer> relayBatch(int batchSize) {
        int effectiveBatchSize = batchSize <= 0 ? relayProperties.getBatchSize() : batchSize;
        Instant now = Instant.now();
        Timer.Sample sample = Timer.start(meterRegistry);
        return outboxEventRepository.findReadyForRelay(now, effectiveBatchSize)
                .flatMapSequential(this::relaySingle, Math.max(1, relayProperties.getDeliveryConcurrency()), 1)
                .count()
                .map(Long::intValue)
                .doOnSuccess(ignored -> sample.stop(relayBatchTimer))
                .doOnError(error -> sample.stop(relayBatchTimer));
    }

    private Mono<OutboxEvent> relaySingle(OutboxEvent event) {
        Instant attemptedAt = Instant.now();
        return Flux.fromIterable(deliverySinks)
                .concatMap(deliverySink -> deliverySink.deliver(event))
                .then(outboxEventRepository.save(event.markPublished(attemptedAt))
                        .doOnNext(saved -> relayPublishedCounter.increment()))
                .onErrorResume(error -> {
                    relayFailedCounter.increment();
                    return outboxEventRepository.save(handleFailure(event, attemptedAt, error));
                });
    }

    private OutboxEvent handleFailure(OutboxEvent event, Instant attemptedAt, Throwable error) {
        String message = error == null ? "Unknown relay failure" : error.getMessage();
        int nextAttemptCount = event.attemptCount() + 1;
        if (nextAttemptCount >= relayProperties.getMaxAttempts()) {
            return event.markDeadLetter(attemptedAt, message);
        }
        Duration backoff = computeBackoff(nextAttemptCount);
        return event.scheduleRetry(attemptedAt, attemptedAt.plus(backoff), message);
    }

    private Duration computeBackoff(int nextAttemptCount) {
        double multiplierExponent = Math.max(0, nextAttemptCount - 1);
        double computedMillis = relayProperties.getInitialBackoff().toMillis()
                * Math.pow(relayProperties.getBackoffMultiplier(), multiplierExponent);
        long boundedMillis = Math.min((long) computedMillis, relayProperties.getMaxBackoff().toMillis());
        return Duration.ofMillis(Math.max(1000L, boundedMillis));
    }
}
