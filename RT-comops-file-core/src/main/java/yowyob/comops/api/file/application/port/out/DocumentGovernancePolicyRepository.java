package yowyob.comops.api.file.application.port.out;

import yowyob.comops.api.file.domain.model.DocumentGovernancePolicy;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentGovernancePolicyRepository {
    Mono<DocumentGovernancePolicy> save(DocumentGovernancePolicy policy);
    Mono<DocumentGovernancePolicy> findByScope(UUID tenantId, UUID organizationId, UUID agencyId, String targetType,
            String documentCategory);
    Flux<DocumentGovernancePolicy> findByOrganization(UUID tenantId, UUID organizationId);
}
