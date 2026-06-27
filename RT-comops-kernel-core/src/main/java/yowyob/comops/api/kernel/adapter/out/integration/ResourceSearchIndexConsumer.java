package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.adapter.out.search.ResourceSearchDocument;
import yowyob.comops.api.kernel.adapter.out.search.ResourceSearchDocumentRepository;
import yowyob.comops.api.kernel.application.port.out.BusinessEventConsumer;
import yowyob.comops.api.kernel.config.ElasticsearchSearchProperties;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "iwm.search.elasticsearch", name = "enabled", havingValue = "true")
public class ResourceSearchIndexConsumer implements BusinessEventConsumer {

    private static final Set<String> RESOURCE_STATUS_EVENTS = Set.of(
            "RESOURCE_RESERVED",
            "RESOURCE_RESERVATION_RELEASED",
            "RESOURCE_ASSIGNED",
            "RESOURCE_UNASSIGNED",
            "RESOURCE_DISPOSED");

    private final ResourceSearchDocumentRepository repository;

    public ResourceSearchIndexConsumer(ResourceSearchDocumentRepository repository,
            ElasticsearchSearchProperties properties) {
        this.repository = repository;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return "MATERIAL_RESOURCE_REGISTERED".equals(event.eventType())
                || RESOURCE_STATUS_EVENTS.contains(event.eventType());
    }

    @Override
    public Mono<Void> consume(OutboxEvent event) {
        if ("MATERIAL_RESOURCE_REGISTERED".equals(event.eventType())) {
            return repository.save(toDocument(event)).then();
        }
        return repository.findById(event.aggregateId())
                .flatMap(existing -> repository.save(existing.withStatus(resolveStatus(event.payload(), existing.status()))))
                .then();
    }

    private ResourceSearchDocument toDocument(OutboxEvent event) {
        Map<String, Object> payload = event.payload();
        return new ResourceSearchDocument(
                event.aggregateId(),
                event.tenantId(),
                event.organizationId(),
                uuidValue(payload.get("agencyId")),
                stringValue(payload, "resourceCode"),
                stringValue(payload, "name"),
                stringValue(payload, "category"),
                stringValue(payload, "serialNumber"),
                stringValue(payload, "status"),
                stringValue(payload, "ipAddress"),
                stringValue(payload, "macAddress"),
                doubleValue(payload.get("latitude")),
                doubleValue(payload.get("longitude")));
    }

    private String resolveStatus(Map<String, Object> payload, String fallback) {
        Object resourceStatus = payload.get("resourceStatus");
        if (resourceStatus != null) {
            return resourceStatus.toString();
        }
        Object status = payload.get("status");
        return status == null ? fallback : status.toString();
    }

    private UUID uuidValue(Object value) {
        return value == null ? null : UUID.fromString(value.toString());
    }

    private Double doubleValue(Object value) {
        return value == null ? null : Double.valueOf(value.toString());
    }

    private String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }
}
