package yowyob.comops.api.file.application.service;

import yowyob.comops.api.file.application.port.out.DocumentLinkRepository;
import yowyob.comops.api.file.application.port.out.StoredFileRepository;
import yowyob.comops.api.file.domain.model.DocumentLink;
import yowyob.comops.api.file.domain.model.StoredFile;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DocumentHubApplicationService {

    private final DocumentLinkRepository documentLinkRepository;
    private final StoredFileRepository storedFileRepository;

    public DocumentHubApplicationService(DocumentLinkRepository documentLinkRepository,
            StoredFileRepository storedFileRepository) {
        this.documentLinkRepository = documentLinkRepository;
        this.storedFileRepository = storedFileRepository;
    }

    public Mono<DocumentLinkView> attach(UUID tenantId, UUID organizationId, UUID userId, String targetType, UUID targetId,
            UUID fileId, String documentCategory, String label) {
        if (organizationId == null) {
            return Mono.error(new IllegalArgumentException("organizationId is required in X-Organization-Id header"));
        }
        return storedFileRepository.findById(tenantId, fileId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("fileId does not reference an existing file")))
                .flatMap(file -> {
                    if (file.organizationId() != null && !file.organizationId().equals(organizationId)) {
                        return Mono.error(new IllegalArgumentException("file belongs to another organization"));
                    }
                    return documentLinkRepository.save(DocumentLink.attach(tenantId, organizationId, targetType,
                                    targetId, fileId, documentCategory, label, userId))
                            .map(link -> toView(link, file));
                });
    }

    public Flux<DocumentLinkView> listByTarget(UUID tenantId, String targetType, UUID targetId) {
        return documentLinkRepository.findByTarget(tenantId, normalize(targetType, "targetType"), targetId)
                .flatMap(this::toView);
    }

    public Flux<DocumentLinkView> listByOrganization(UUID tenantId, UUID organizationId) {
        return documentLinkRepository.findByOrganization(tenantId, organizationId)
                .flatMap(this::toView);
    }

    public Mono<DocumentHubOverview> overview(UUID tenantId, UUID organizationId) {
        return documentLinkRepository.findByOrganization(tenantId, organizationId)
                .collectList()
                .map(links -> {
                    Map<String, Long> countsByTargetType = new LinkedHashMap<>();
                    Map<String, Long> countsByCategory = new LinkedHashMap<>();
                    for (DocumentLink link : links) {
                        countsByTargetType.merge(link.targetType(), 1L, Long::sum);
                        countsByCategory.merge(link.documentCategory(), 1L, Long::sum);
                    }
                    return new DocumentHubOverview(organizationId, links.size(), countsByTargetType, countsByCategory);
                });
    }

    private Mono<DocumentLinkView> toView(DocumentLink link) {
        return storedFileRepository.findById(link.tenantId(), link.fileId())
                .map(file -> toView(link, file));
    }

    private DocumentLinkView toView(DocumentLink link, StoredFile file) {
        return new DocumentLinkView(link.id(), link.organizationId(), link.targetType(), link.targetId(), link.fileId(),
                file.fileName(), file.contentType(), file.size(), link.documentCategory(), link.label(),
                link.attachedByUserId(), link.attachedAt());
    }

    private String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase();
    }

    public record DocumentLinkView(UUID id, UUID organizationId, String targetType, UUID targetId, UUID fileId,
            String fileName, String contentType, long fileSize, String documentCategory, String label,
            UUID attachedByUserId, Instant attachedAt) {
    }

    public record DocumentHubOverview(UUID organizationId, int totalDocuments, Map<String, Long> countsByTargetType,
            Map<String, Long> countsByCategory) {
    }
}
