package yowyob.comops.api.file.adapter.out.persistence;

import yowyob.comops.api.file.application.port.out.DocumentGovernancePolicyRepository;
import yowyob.comops.api.file.domain.model.DocumentGovernancePolicy;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class DocumentGovernancePolicyR2dbcRepositoryAdapter implements DocumentGovernancePolicyRepository {

    private final DocumentGovernancePolicySpringDataRepository repository;

    public DocumentGovernancePolicyR2dbcRepositoryAdapter(DocumentGovernancePolicySpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<DocumentGovernancePolicy> save(DocumentGovernancePolicy policy) {
        return repository.save(toEntity(policy)).map(this::toDomain);
    }

    @Override
    public Mono<DocumentGovernancePolicy> findByScope(UUID tenantId, UUID organizationId, UUID agencyId,
            String targetType, String documentCategory) {
        Mono<DocumentGovernancePolicyEntity> scoped = agencyId == null
                ? Mono.empty()
                : repository.findByTenantIdAndOrganizationIdAndAgencyIdAndTargetTypeAndDocumentCategory(tenantId,
                        organizationId, agencyId, targetType.toUpperCase(), documentCategory.toUpperCase());
        return scoped.switchIfEmpty(repository.findByTenantIdAndOrganizationIdAndAgencyIdIsNullAndTargetTypeAndDocumentCategory(
                tenantId, organizationId, targetType.toUpperCase(), documentCategory.toUpperCase()))
                .map(this::toDomain);
    }

    @Override
    public Flux<DocumentGovernancePolicy> findByOrganization(UUID tenantId, UUID organizationId) {
        return repository.findAllByTenantIdAndOrganizationId(tenantId, organizationId).map(this::toDomain);
    }

    private DocumentGovernancePolicyEntity toEntity(DocumentGovernancePolicy policy) {
        return new DocumentGovernancePolicyEntity(policy.id(), policy.tenantId(), policy.createdAt(),
                policy.updatedAt(), policy.organizationId(), policy.agencyId(), policy.targetType(),
                policy.documentCategory(), policy.mandatory(), policy.approvalRequired(), policy.expiryDays(),
                policy.reviewerResponsibilityType());
    }

    private DocumentGovernancePolicy toDomain(DocumentGovernancePolicyEntity entity) {
        return DocumentGovernancePolicy.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(),
                entity.updatedAt(), entity.organizationId(), entity.agencyId(), entity.targetType(),
                entity.documentCategory(), entity.mandatory(), entity.approvalRequired(), entity.expiryDays(),
                entity.reviewerResponsibilityType());
    }
}
