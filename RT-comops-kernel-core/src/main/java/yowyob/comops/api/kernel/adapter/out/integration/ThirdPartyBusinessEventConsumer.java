package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ThirdPartyBusinessEventConsumer extends AbstractProjectionConsumer {

    public ThirdPartyBusinessEventConsumer(DomainEventProjectionRepository repository) {
        super(repository, Set.of("THIRD_PARTY_CREATED", "THIRD_PARTY_UPDATED", "THIRD_PARTY_CONVERTED_TO_CUSTOMER",
                "THIRD_PARTY_ACTIVATED", "THIRD_PARTY_DEACTIVATED", "THIRD_PARTY_ACCOUNTING_ACCOUNT_UPDATED",
                "THIRD_PARTY_QUALIFIED", "THIRD_PARTY_SCORE_RECOMPUTED", "THIRD_PARTY_FOLLOW_UP_SCHEDULED",
                "THIRD_PARTY_FOLLOW_UP_COMPLETED", "THIRD_PARTY_DELETED"), "THIRD_PARTY");
    }

    @Override
    protected String businessKey(OutboxEvent event) {
        for (String key : Set.of("referenceCode", "displayName", "name")) {
            Object value = event.payload().get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    @Override
    protected String lifecycleStatus(OutboxEvent event) {
        Object prospect = event.payload().get("prospect");
        if (prospect != null) {
            return Boolean.parseBoolean(prospect.toString()) ? "PROSPECT" : "QUALIFIED";
        }
        return event.eventType();
    }
}
