package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class InventoryBusinessEventConsumer extends AbstractProjectionConsumer {

    public InventoryBusinessEventConsumer(DomainEventProjectionRepository repository) {
        super(repository, Set.of("STOCK_MOVEMENT_RECORDED", "PRODUCT_TRANSFORMATION_RECORDED",
                "WAREHOUSE_TRANSFER_CREATED", "WAREHOUSE_TRANSFER_COMPLETED", "SALES_ORDER_STOCK_DISPATCHED"),
                "INVENTORY");
    }

    @Override
    protected String businessKey(OutboxEvent event) {
        Object reference = event.payload().get("referenceNumber");
        if (reference != null) {
            return reference.toString();
        }
        Object sourceDocumentNumber = event.payload().get("sourceDocumentNumber");
        if (sourceDocumentNumber != null) {
            return sourceDocumentNumber.toString();
        }
        Object salesOrderNumber = event.payload().get("salesOrderNumber");
        return salesOrderNumber == null ? null : salesOrderNumber.toString();
    }

    @Override
    protected String lifecycleStatus(OutboxEvent event) {
        Object status = event.payload().get("status");
        return status == null ? event.eventType() : status.toString();
    }
}
