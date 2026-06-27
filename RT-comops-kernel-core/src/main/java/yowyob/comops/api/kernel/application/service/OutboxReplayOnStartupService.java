package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.kernel.application.port.out.BusinessEventConsumer;
import yowyob.comops.api.kernel.application.port.out.OutboxEventRepository;
import yowyob.comops.api.kernel.config.OutboxReplayProperties;
import yowyob.comops.api.kernel.domain.model.OutboxEventStatus;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;

@Service
@ConditionalOnProperty(prefix = "iwm.outbox.replay-on-startup", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class OutboxReplayOnStartupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxReplayOnStartupService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final List<BusinessEventConsumer> consumers;
    private final OutboxReplayProperties properties;
    private final AtomicBoolean replayInProgress = new AtomicBoolean(false);

    public OutboxReplayOnStartupService(OutboxEventRepository outboxEventRepository,
            List<BusinessEventConsumer> consumers,
            OutboxReplayProperties properties) {
        this.outboxEventRepository = outboxEventRepository;
        this.consumers = consumers;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void replayPublishedEvents() {
        if (!properties.isEnabled() || !replayInProgress.compareAndSet(false, true)) {
            return;
        }
        outboxEventRepository.findByStatus(OutboxEventStatus.PUBLISHED, properties.getMaxEvents())
                .flatMap(this::deliverToMatchingConsumers, Math.max(1, properties.getConsumerConcurrency()))
                .count()
                .doOnSuccess(count -> LOGGER.info("Outbox startup replay completed. replayedEvents={}", count))
                .doOnError(error -> LOGGER.error("Outbox startup replay failed.", error))
                .onErrorResume(error -> Mono.empty())
                .doFinally(signal -> replayInProgress.set(false))
                .subscribe();
    }

    private Mono<Void> deliverToMatchingConsumers(OutboxEvent event) {
        List<BusinessEventConsumer> matchingConsumers = consumers.stream()
                .filter(consumer -> consumer.supports(event))
                .toList();
        if (matchingConsumers.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(matchingConsumers)
                .concatMap(consumer -> consumer.consume(event))
                .then();
    }
}
