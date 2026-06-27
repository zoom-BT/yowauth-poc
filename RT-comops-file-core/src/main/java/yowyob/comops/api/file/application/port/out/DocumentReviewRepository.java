package yowyob.comops.api.file.application.port.out;

import yowyob.comops.api.file.domain.model.DocumentReview;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentReviewRepository {
    Mono<DocumentReview> save(DocumentReview review);
    Flux<DocumentReview> findByOrganization(UUID tenantId, UUID organizationId);
    Flux<DocumentReview> findByDocumentLinkId(UUID tenantId, UUID documentLinkId);
}
