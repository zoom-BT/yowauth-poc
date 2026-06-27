package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ResourceBusinessEventConsumer extends AbstractProjectionConsumer {

    public ResourceBusinessEventConsumer(DomainEventProjectionRepository repository) {
        super(repository, Set.of(
                "MATERIAL_RESOURCE_REGISTERED",
                "RESOURCE_RESERVED",
                "RESOURCE_RESERVATION_RELEASED",
                "RESOURCE_ASSIGNED",
                "RESOURCE_UNASSIGNED",
                "RESOURCE_DISPOSED"), "RESOURCE");
    }

    @Override
    protected String businessKey(OutboxEvent event) {
        Object resourceCode = event.payload().get("resourceCode");
        return resourceCode == null ? null : resourceCode.toString();
    }

    @Override
    protected String lifecycleStatus(OutboxEvent event) {
        Object resourceStatus = event.payload().get("resourceStatus");
        if (resourceStatus != null) {
            return resourceStatus.toString();
        }
        Object status = event.payload().get("status");
        return status == null ? event.eventType() : status.toString();
    }
}
