package yowyob.comops.api.file.adapter.out.persistence;

import yowyob.comops.api.file.application.port.out.DocumentGovernancePolicyRepository;
import yowyob.comops.api.file.domain.model.DocumentGovernancePolicy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryDocumentGovernancePolicyRepository implements DocumentGovernancePolicyRepository {

    private final Map<UUID, DocumentGovernancePolicy> store = new ConcurrentHashMap<>();

    @Override
    public Mono<DocumentGovernancePolicy> save(DocumentGovernancePolicy policy) {
        return Mono.fromSupplier(() -> {
            store.put(policy.id(), policy);
            return policy;
        });
    }

    @Override
    public Mono<DocumentGovernancePolicy> findByScope(UUID tenantId, UUID organizationId, UUID agencyId,
            String targetType, String documentCategory) {
        String normalizedTargetType = targetType.toUpperCase();
        String normalizedCategory = documentCategory.toUpperCase();
        Mono<DocumentGovernancePolicy> scoped = Flux.fromStream(store.values().stream()
                        .filter(policy -> policy.tenantId().equals(tenantId))
                        .filter(policy -> policy.organizationId().equals(organizationId))
                        .filter(policy -> agencyId != null && agencyId.equals(policy.agencyId()))
                        .filter(policy -> policy.targetType().equals(normalizedTargetType))
                        .filter(policy -> policy.documentCategory().equals(normalizedCategory)))
                .next();
        return scoped.switchIfEmpty(Flux.fromStream(store.values().stream()
                        .filter(policy -> policy.tenantId().equals(tenantId))
                        .filter(policy -> policy.organizationId().equals(organizationId))
                        .filter(policy -> policy.agencyId() == null)
                        .filter(policy -> policy.targetType().equals(normalizedTargetType))
                        .filter(policy -> policy.documentCategory().equals(normalizedCategory)))
                .next());
    }

    @Override
    public Flux<DocumentGovernancePolicy> findByOrganization(UUID tenantId, UUID organizationId) {
        return Flux.fromStream(store.values().stream()
                .filter(policy -> policy.tenantId().equals(tenantId))
                .filter(policy -> policy.organizationId().equals(organizationId)));
    }
}
