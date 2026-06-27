package yowyob.comops.api.file.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DocumentLinkSpringDataRepository extends ReactiveCrudRepository<DocumentLinkEntity, UUID> {
    reactor.core.publisher.Mono<DocumentLinkEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    Flux<DocumentLinkEntity> findAllByTenantIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(UUID tenantId,
            String targetType, UUID targetId);

    Flux<DocumentLinkEntity> findAllByTenantIdAndOrganizationIdOrderByCreatedAtDesc(UUID tenantId, UUID organizationId);
}
