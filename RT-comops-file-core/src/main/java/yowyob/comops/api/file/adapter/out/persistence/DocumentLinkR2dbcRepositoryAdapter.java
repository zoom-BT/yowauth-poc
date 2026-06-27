package yowyob.comops.api.file.adapter.out.persistence;

import yowyob.comops.api.file.application.port.out.DocumentLinkRepository;
import yowyob.comops.api.file.domain.model.DocumentLink;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class DocumentLinkR2dbcRepositoryAdapter implements DocumentLinkRepository {

    private final DocumentLinkSpringDataRepository repository;

    public DocumentLinkR2dbcRepositoryAdapter(DocumentLinkSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<DocumentLink> save(DocumentLink documentLink) {
        return repository.save(toEntity(documentLink)).map(this::toDomain);
    }

    @Override
    public Mono<DocumentLink> findById(UUID tenantId, UUID documentLinkId) {
        return repository.findByIdAndTenantId(documentLinkId, tenantId).map(this::toDomain);
    }

    @Override
    public Flux<DocumentLink> findByTarget(UUID tenantId, String targetType, UUID targetId) {
        return repository.findAllByTenantIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(
                        tenantId, targetType.toUpperCase(), targetId)
                .map(this::toDomain);
    }

    @Override
    public Flux<DocumentLink> findByOrganization(UUID tenantId, UUID organizationId) {
        return repository.findAllByTenantIdAndOrganizationIdOrderByCreatedAtDesc(tenantId, organizationId)
                .map(this::toDomain);
    }

    private DocumentLinkEntity toEntity(DocumentLink documentLink) {
        return new DocumentLinkEntity(documentLink.id(), documentLink.tenantId(), documentLink.createdAt(),
                documentLink.updatedAt(), documentLink.organizationId(), documentLink.targetType(),
                documentLink.targetId(), documentLink.fileId(), documentLink.documentCategory(), documentLink.label(),
                documentLink.attachedByUserId(), documentLink.attachedAt());
    }

    private DocumentLink toDomain(DocumentLinkEntity entity) {
        return DocumentLink.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(), entity.updatedAt(),
                entity.organizationId(), entity.targetType(), entity.targetId(), entity.fileId(),
                entity.documentCategory(), entity.label(), entity.attachedByUserId(), entity.attachedAt());
    }
}
