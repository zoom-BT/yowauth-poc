package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.adapter.out.search.ThirdPartySearchDocument;
import yowyob.comops.api.kernel.adapter.out.search.ThirdPartySearchDocumentRepository;
import yowyob.comops.api.kernel.application.port.out.BusinessEventConsumer;
import yowyob.comops.api.kernel.config.ElasticsearchSearchProperties;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "iwm.search.elasticsearch", name = "enabled", havingValue = "true")
public class ThirdPartySearchIndexConsumer implements BusinessEventConsumer {

    private final ThirdPartySearchDocumentRepository repository;

    public ThirdPartySearchIndexConsumer(ThirdPartySearchDocumentRepository repository,
            ElasticsearchSearchProperties properties) {
        this.repository = repository;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return switch (event.eventType()) {
            case "THIRD_PARTY_CREATED", "THIRD_PARTY_UPDATED", "THIRD_PARTY_CONVERTED_TO_CUSTOMER",
                    "THIRD_PARTY_ACTIVATED", "THIRD_PARTY_DEACTIVATED", "THIRD_PARTY_ACCOUNTING_ACCOUNT_UPDATED",
                    "THIRD_PARTY_QUALIFIED", "THIRD_PARTY_SCORE_RECOMPUTED",
                    "THIRD_PARTY_FOLLOW_UP_SCHEDULED", "THIRD_PARTY_FOLLOW_UP_COMPLETED" ->
                true;
            case "THIRD_PARTY_DELETED" -> true;
            default -> false;
        };
    }

    @Override
    public Mono<Void> consume(OutboxEvent event) {
        if ("THIRD_PARTY_DELETED".equals(event.eventType())) {
            return repository.deleteById(event.aggregateId());
        }
        Map<String, Object> payload = event.payload();
        ThirdPartySearchDocument document = new ThirdPartySearchDocument(
                event.aggregateId(),
                event.tenantId(),
                event.organizationId(),
                stringValue(payload, "code"),
                stringValue(payload, "referenceCode"),
                stringValue(payload, "name"),
                stringValue(payload, "displayName"),
                stringValue(payload, "type"),
                stringValue(payload, "longName"),
                roles(payload.get("roles")),
                booleanValue(payload.get("prospect")),
                stringValue(payload, "accountingAccount"),
                stringValue(payload, "segment"),
                integerValue(payload.get("qualificationScore")),
                booleanValue(payload.get("enabled")),
                booleanValue(payload.get("active")),
                instantValue(payload.get("lastContactedAt")),
                instantValue(payload.get("nextFollowUpAt")),
                stringValue(payload, "followUpStatus"),
                instantValue(payload.get("convertedAt")),
                stringValue(payload, "partyType"),
                uuidValue(payload.get("partyId")));
        return repository.save(document).then();
    }

    private Set<String> roles(Object value) {
        if (value instanceof Iterable<?> iterable) {
            LinkedHashSet<String> roles = new LinkedHashSet<>();
            for (Object role : iterable) {
                if (role != null) {
                    roles.add(role.toString());
                }
            }
            return Set.copyOf(roles);
        }
        return Set.of();
    }

    private boolean booleanValue(Object value) {
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private UUID uuidValue(Object value) {
        return value == null ? null : UUID.fromString(value.toString());
    }

    private Integer integerValue(Object value) {
        return value == null ? null : Integer.valueOf(value.toString());
    }

    private String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }

    private Instant instantValue(Object value) {
        return value == null ? null : Instant.parse(value.toString());
    }
}
