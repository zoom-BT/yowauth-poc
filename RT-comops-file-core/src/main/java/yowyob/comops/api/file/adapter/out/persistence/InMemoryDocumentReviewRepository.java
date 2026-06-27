package yowyob.comops.api.file.adapter.out.persistence;

import yowyob.comops.api.file.application.port.out.DocumentReviewRepository;
import yowyob.comops.api.file.domain.model.DocumentReview;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryDocumentReviewRepository implements DocumentReviewRepository {

    private final Map<UUID, DocumentReview> store = new ConcurrentHashMap<>();

    @Override
    public Mono<DocumentReview> save(DocumentReview review) {
        return Mono.fromSupplier(() -> {
            store.put(review.id(), review);
            return review;
        });
    }

    @Override
    public Flux<DocumentReview> findByOrganization(UUID tenantId, UUID organizationId) {
        return Flux.fromStream(store.values().stream()
                .filter(review -> review.tenantId().equals(tenantId))
                .filter(review -> review.organizationId().equals(organizationId)));
    }

    @Override
    public Flux<DocumentReview> findByDocumentLinkId(UUID tenantId, UUID documentLinkId) {
        return Flux.fromStream(store.values().stream()
                .filter(review -> review.tenantId().equals(tenantId))
                .filter(review -> review.documentLinkId().equals(documentLinkId))
                .sorted((left, right) -> right.reviewedAt().compareTo(left.reviewedAt())));
    }
}
