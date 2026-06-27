package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.application.port.out.BusinessEventDeliverySink;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Order(1)
@ConditionalOnProperty(prefix = "iwm.outbox.delivery", name = "type", havingValue = "recording")
public class RecordingBusinessEventDeliverySink implements BusinessEventDeliverySink {

    private final ConcurrentMap<UUID, OutboxEvent> deliveredEvents = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> deliver(OutboxEvent event) {
        return Mono.fromRunnable(() -> deliveredEvents.put(event.id(), event));
    }

    public List<OutboxEvent> deliveredEvents() {
        return deliveredEvents.values().stream()
                .sorted(Comparator.comparing(OutboxEvent::createdAt))
                .toList();
    }
}
