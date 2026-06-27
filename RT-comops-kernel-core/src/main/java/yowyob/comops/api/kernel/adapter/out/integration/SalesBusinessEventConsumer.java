package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SalesBusinessEventConsumer extends AbstractProjectionConsumer {

    public SalesBusinessEventConsumer(DomainEventProjectionRepository repository) {
        super(repository, Set.of("SALES_ORDER_CREATED", "SALES_ORDER_CONFIRMED"), "SALES");
    }

    @Override
    protected String businessKey(OutboxEvent event) {
        return stringValue(event, "orderNumber");
    }

    @Override
    protected String lifecycleStatus(OutboxEvent event) {
        return stringValue(event, "status");
    }

    private String stringValue(OutboxEvent event, String key) {
        Object value = event.payload().get(key);
        return value == null ? null : value.toString();
    }
}
