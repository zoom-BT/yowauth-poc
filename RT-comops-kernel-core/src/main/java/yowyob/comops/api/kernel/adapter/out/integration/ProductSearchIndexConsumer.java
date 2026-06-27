package yowyob.comops.api.kernel.adapter.out.integration;

import yowyob.comops.api.kernel.adapter.out.search.ProductSearchDocument;
import yowyob.comops.api.kernel.adapter.out.search.ProductSearchDocumentRepository;
import yowyob.comops.api.kernel.application.port.out.BusinessEventConsumer;
import yowyob.comops.api.kernel.config.ElasticsearchSearchProperties;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "iwm.search.elasticsearch", name = "enabled", havingValue = "true")
public class ProductSearchIndexConsumer implements BusinessEventConsumer {

    private final ProductSearchDocumentRepository repository;

    public ProductSearchIndexConsumer(ProductSearchDocumentRepository repository,
            ElasticsearchSearchProperties properties) {
        this.repository = repository;
    }

    @Override
    public boolean supports(OutboxEvent event) {
        return "PRODUCT_CREATED".equals(event.eventType());
    }

    @Override
    public Mono<Void> consume(OutboxEvent event) {
        Map<String, Object> payload = event.payload();
        ProductSearchDocument document = new ProductSearchDocument(
                event.aggregateId(),
                event.tenantId(),
                event.organizationId(),
                stringValue(payload, "sku"),
                stringValue(payload, "name"),
                stringValue(payload, "familyCode"),
                stringValue(payload, "variantLabel"),
                stringValue(payload, "barcode"),
                stringValue(payload, "description"),
                decimalValue(payload.get("unitPrice")),
                stringValue(payload, "currency"),
                stringValue(payload, "status"));
        return repository.save(document).then();
    }

    private java.math.BigDecimal decimalValue(Object value) {
        return value == null ? null : new java.math.BigDecimal(value.toString());
    }

    private String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }
}
