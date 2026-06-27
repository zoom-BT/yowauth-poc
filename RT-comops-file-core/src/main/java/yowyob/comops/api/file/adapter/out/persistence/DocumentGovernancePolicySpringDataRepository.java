package yowyob.comops.api.file.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentGovernancePolicySpringDataRepository extends ReactiveCrudRepository<DocumentGovernancePolicyEntity, UUID> {
    Mono<DocumentGovernancePolicyEntity> findByTenantIdAndOrganizationIdAndAgencyIdAndTargetTypeAndDocumentCategory(
            UUID tenantId, UUID organizationId, UUID agencyId, String targetType, String documentCategory);
    Mono<DocumentGovernancePolicyEntity> findByTenantIdAndOrganizationIdAndAgencyIdIsNullAndTargetTypeAndDocumentCategory(
            UUID tenantId, UUID organizationId, String targetType, String documentCategory);
    Flux<DocumentGovernancePolicyEntity> findAllByTenantIdAndOrganizationId(UUID tenantId, UUID organizationId);
}
