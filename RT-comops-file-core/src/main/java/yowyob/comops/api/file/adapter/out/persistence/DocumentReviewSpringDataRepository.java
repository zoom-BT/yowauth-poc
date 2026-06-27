package yowyob.comops.api.file.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DocumentReviewSpringDataRepository extends ReactiveCrudRepository<DocumentReviewEntity, UUID> {
    Flux<DocumentReviewEntity> findAllByTenantIdAndOrganizationId(UUID tenantId, UUID organizationId);
    Flux<DocumentReviewEntity> findAllByTenantIdAndDocumentLinkIdOrderByReviewedAtDesc(UUID tenantId,
            UUID documentLinkId);
}
