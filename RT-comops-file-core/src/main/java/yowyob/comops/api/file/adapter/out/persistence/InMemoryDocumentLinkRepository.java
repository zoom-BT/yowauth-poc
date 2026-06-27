package yowyob.comops.api.file.adapter.out.persistence;

import yowyob.comops.api.file.application.port.out.DocumentLinkRepository;
import yowyob.comops.api.file.domain.model.DocumentLink;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryDocumentLinkRepository implements DocumentLinkRepository {

    private final Map<UUID, DocumentLink> store = new ConcurrentHashMap<>();

    @Override
    public Mono<DocumentLink> save(DocumentLink documentLink) {
        return Mono.fromSupplier(() -> {
            store.put(documentLink.id(), documentLink);
            return documentLink;
        });
    }

    @Override
    public Mono<DocumentLink> findById(UUID tenantId, UUID documentLinkId) {
        return Mono.justOrEmpty(store.get(documentLinkId))
                .filter(link -> link.tenantId().equals(tenantId));
    }

    @Override
    public Flux<DocumentLink> findByTarget(UUID tenantId, String targetType, UUID targetId) {
        String normalizedType = targetType == null ? null : targetType.trim().toUpperCase();
        return Flux.fromStream(store.values().stream()
                .filter(link -> link.tenantId().equals(tenantId))
                .filter(link -> link.targetId().equals(targetId))
                .filter(link -> link.targetType().equals(normalizedType))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt())));
    }

    @Override
    public Flux<DocumentLink> findByOrganization(UUID tenantId, UUID organizationId) {
        return Flux.fromStream(store.values().stream()
                .filter(link -> link.tenantId().equals(tenantId))
                .filter(link -> link.organizationId().equals(organizationId))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt())));
    }
}
