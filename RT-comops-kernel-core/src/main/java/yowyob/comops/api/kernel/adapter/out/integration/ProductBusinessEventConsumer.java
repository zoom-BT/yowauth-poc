package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProductBusinessEventConsumer extends AbstractProjectionConsumer {

    public ProductBusinessEventConsumer(DomainEventProjectionRepository repository) {
        super(repository, Set.of("PRODUCT_CREATED"), "PRODUCT");
    }

    @Override
    protected String businessKey(OutboxEvent event) {
        for (String key : Set.of("sku", "barcode", "name")) {
            Object value = event.payload().get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    @Override
    protected String lifecycleStatus(OutboxEvent event) {
        Object status = event.payload().get("status");
        return status == null ? event.eventType() : status.toString();
    }
}
