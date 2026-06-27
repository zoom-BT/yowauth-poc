package yowyob.comops.api.kernel.config;

import yowyob.comops.api.kernel.application.port.in.RelayOutboxEventsUseCase;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "iwm.outbox.relay", name = "enabled", havingValue = "true")
public class OutboxRelayScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final RelayOutboxEventsUseCase relayOutboxEventsUseCase;
    private final OutboxRelayProperties relayProperties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public OutboxRelayScheduler(RelayOutboxEventsUseCase relayOutboxEventsUseCase,
            OutboxRelayProperties relayProperties) {
        this.relayOutboxEventsUseCase = relayOutboxEventsUseCase;
        this.relayProperties = relayProperties;
    }

    @Scheduled(fixedDelayString = "${iwm.outbox.relay.fixed-delay:PT5S}", timeUnit = TimeUnit.MILLISECONDS)
    public void relayBatch() {
        if (!running.compareAndSet(false, true)) {
            LOGGER.debug("outbox relay batch skipped because previous batch is still running");
            return;
        }
        relayOutboxEventsUseCase.relayBatch(relayProperties.getBatchSize())
                .doOnError(error -> LOGGER.error("outbox relay batch failed", error))
                .doFinally(signalType -> running.set(false))
                .subscribe(relayedCount -> LOGGER.debug("outbox relay batch relayed {} events", relayedCount));
    }
}
