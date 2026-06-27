package yowyob.comops.api.file.adapter.out.persistence;

import yowyob.comops.api.file.application.port.out.DocumentReviewRepository;
import yowyob.comops.api.file.domain.model.DocumentReview;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class DocumentReviewR2dbcRepositoryAdapter implements DocumentReviewRepository {

    private final DocumentReviewSpringDataRepository repository;

    public DocumentReviewR2dbcRepositoryAdapter(DocumentReviewSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<DocumentReview> save(DocumentReview review) {
        return repository.save(toEntity(review)).map(this::toDomain);
    }

    @Override
    public Flux<DocumentReview> findByOrganization(UUID tenantId, UUID organizationId) {
        return repository.findAllByTenantIdAndOrganizationId(tenantId, organizationId).map(this::toDomain);
    }

    @Override
    public Flux<DocumentReview> findByDocumentLinkId(UUID tenantId, UUID documentLinkId) {
        return repository.findAllByTenantIdAndDocumentLinkIdOrderByReviewedAtDesc(tenantId, documentLinkId)
                .map(this::toDomain);
    }

    private DocumentReviewEntity toEntity(DocumentReview review) {
        return new DocumentReviewEntity(review.id(), review.tenantId(), review.createdAt(), review.updatedAt(),
                review.organizationId(), review.documentLinkId(), review.reviewerUserId(), review.reviewStatus(),
                review.reviewedAt(), review.expiresAt(), review.notes());
    }

    private DocumentReview toDomain(DocumentReviewEntity entity) {
        return DocumentReview.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(), entity.updatedAt(),
                entity.organizationId(), entity.documentLinkId(), entity.reviewerUserId(), entity.reviewStatus(),
                entity.reviewedAt(), entity.expiresAt(), entity.notes());
    }
}
