package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.adapter.out.search.OrganizationSearchDocument;
import yowyob.comops.api.kernel.adapter.out.search.OrganizationSearchDocumentRepository;
import yowyob.comops.api.kernel.application.port.out.BusinessEventConsumer;
import yowyob.comops.api.kernel.config.ElasticsearchSearchProperties;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "iwm.search.elasticsearch", name = "enabled", havingValue = "true")
public class OrganizationSearchIndexConsumer implements BusinessEventConsumer {

    private final OrganizationSearchDocumentRepository repository;

    public OrganizationSearchIndexConsumer(OrganizationSearchDocumentRepository repository,
            ElasticsearchSearchProperties properties) {
        this.repository = repository;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return "ORGANIZATION_CREATED".equals(event.eventType());
    }

    @Override
    public Mono<Void> consume(OutboxEvent event) {
        Map<String, Object> payload = event.payload();
        OrganizationSearchDocument document = new OrganizationSearchDocument(
                event.aggregateId(),
                event.tenantId(),
                uuidValue(payload.get("businessActorId")),
                stringValue(payload, "code"),
                stringValue(payload, "service"),
                stringValue(payload, "shortName"),
                stringValue(payload, "longName"),
                stringValue(payload, "legalForm"),
                booleanValue(payload.get("isActive")),
                stringValue(payload, "status"));
        return repository.save(document).then();
    }

    private UUID uuidValue(Object value) {
        return value == null ? null : UUID.fromString(value.toString());
    }

    private boolean booleanValue(Object value) {
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }
}
