package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TreasuryBusinessEventConsumer extends AbstractProjectionConsumer {

    public TreasuryBusinessEventConsumer(DomainEventProjectionRepository repository) {
        super(repository, Set.of("BANK_STATEMENT_REGISTERED", "CHECK_PAYMENT_ISSUED", "CHECK_PAYMENT_CLEARED",
                "RECONCILIATION_OPENED", "RECONCILIATION_CLOSED", "INVOICE_SETTLEMENT_REGISTERED"), "TREASURY");
    }

    @Override
    protected String businessKey(OutboxEvent event) {
        for (String key : Set.of("statementNumber", "checkNumber", "referenceNumber", "settlementNumber")) {
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
