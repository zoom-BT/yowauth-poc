package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.application.port.out.DomainEventProjectionRepository;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OrganizationBusinessEventConsumer extends AbstractProjectionConsumer {

    public OrganizationBusinessEventConsumer(DomainEventProjectionRepository repository) {
        super(repository, Set.of("ORGANIZATION_CREATED"), "ORGANIZATION");
    }

    @Override
    protected String businessKey(OutboxEvent event) {
        Object code = event.payload().get("code");
        return code == null ? null : code.toString();
    }

    @Override
    protected String lifecycleStatus(OutboxEvent event) {
        Object organizationType = event.payload().get("organizationType");
        return organizationType == null ? event.eventType() : organizationType.toString();
    }
}
