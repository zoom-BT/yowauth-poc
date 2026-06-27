package yowyob.comops.api.file.application.port.out;

import yowyob.comops.api.file.domain.model.DocumentLink;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentLinkRepository {
    Mono<DocumentLink> save(DocumentLink documentLink);
    Mono<DocumentLink> findById(UUID tenantId, UUID documentLinkId);
    Flux<DocumentLink> findByTarget(UUID tenantId, String targetType, UUID targetId);
    Flux<DocumentLink> findByOrganization(UUID tenantId, UUID organizationId);
}
