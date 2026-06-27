package yowyob.comops.api.file.application.service;

import yowyob.comops.api.file.application.port.in.ApplyFileAnalysisVerdictUseCase;
import yowyob.comops.api.file.application.port.in.GetStoredFileUseCase;
import yowyob.comops.api.file.application.port.in.StoreFileCommand;
import yowyob.comops.api.file.application.port.in.StoreFileUseCase;
import yowyob.comops.api.file.application.port.out.FileBinaryStorage;
import yowyob.comops.api.file.application.port.out.StoredFileRepository;
import yowyob.comops.api.file.config.FileStorageProperties;
import yowyob.comops.api.file.domain.InvalidStoredFileException;
import yowyob.comops.api.file.domain.StoredFileNotAvailableException;
import yowyob.comops.api.file.domain.StoredFileNotFoundException;
import yowyob.comops.api.file.domain.model.FileAnalysisStatus;
import yowyob.comops.api.file.domain.model.FileBusinessEventType;
import yowyob.comops.api.file.domain.model.KycDocumentTypes;
import yowyob.comops.api.file.domain.model.StoredFile;
import yowyob.comops.api.kernel.application.port.in.RecordSystemAuditUseCase;
import yowyob.comops.api.kernel.application.port.out.BusinessEventPublisher;
import yowyob.comops.api.kernel.application.service.ReactiveRequestContextHolder;
import yowyob.comops.api.kernel.domain.model.BusinessEvent;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FileApplicationService
        implements StoreFileUseCase, GetStoredFileUseCase, ApplyFileAnalysisVerdictUseCase {

    private final StoredFileRepository storedFileRepository;
    private final FileBinaryStorage fileBinaryStorage;
    private final RecordSystemAuditUseCase recordSystemAuditUseCase;
    private final BusinessEventPublisher businessEventPublisher;
    private final FileStorageProperties fileStorageProperties;

    public FileApplicationService(StoredFileRepository storedFileRepository, FileBinaryStorage fileBinaryStorage,
            RecordSystemAuditUseCase recordSystemAuditUseCase, BusinessEventPublisher businessEventPublisher,
            FileStorageProperties fileStorageProperties) {
        this.storedFileRepository = storedFileRepository;
        this.fileBinaryStorage = fileBinaryStorage;
        this.recordSystemAuditUseCase = recordSystemAuditUseCase;
        this.businessEventPublisher = businessEventPublisher;
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public Mono<StoredFile> store(StoreFileCommand command) {
        String normalizedFileName = normalizeFileName(command.fileName());
        validate(command, normalizedFileName);
        String documentType = KycDocumentTypes.normalize(command.documentType());
        // Seuls les documents KYC (CNI, passeport…) partent en analyse VerifID, et seulement si
        // l'intégration d'analyse est activée. Tout autre fichier est accepté directement.
        boolean requiresAnalysis = fileStorageProperties.isAnalysisEnabled()
                && KycDocumentTypes.requiresAnalysis(documentType);
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> {
                    String relativePath = context.tenantId() + "/" + UUID.randomUUID() + "/" + normalizedFileName;
                    return fileBinaryStorage.write(relativePath, command.content())
                            .map(storedPath -> requiresAnalysis
                                    ? StoredFile.create(context.tenantId(), context.organizationId(), context.userId(),
                                            normalizedFileName, command.contentType(), command.size(), storedPath,
                                            documentType)
                                    : StoredFile.createAccepted(context.tenantId(), context.organizationId(),
                                            context.userId(), normalizedFileName, command.contentType(),
                                            command.size(), storedPath, documentType))
                            .flatMap(toStore -> storedFileRepository.save(toStore)
                                    .flatMap(saved -> recordSystemAuditUseCase.record(saved.tenantId(),
                                                    saved.organizationId(), saved.uploadedByUserId(),
                                                    FileBusinessEventType.FILE_UPLOADED,
                                                    FileBusinessEventType.AGGREGATE_TYPE, saved.id().toString(),
                                                    saved.fileName())
                                            .then(publishUploaded(saved))
                                            .then(requiresAnalysis ? requestAnalysis(saved) : Mono.empty())
                                            .thenReturn(saved)));
                });
    }

    private Mono<Void> publishUploaded(StoredFile saved) {
        return businessEventPublisher.publish(BusinessEvent.now(
                        saved.tenantId(), saved.organizationId(),
                        FileBusinessEventType.FILE_UPLOADED, FileBusinessEventType.AGGREGATE_TYPE, saved.id(),
                        Map.of("fileName", saved.fileName(),
                                "contentType", saved.contentType(),
                                "size", saved.size())))
                .onErrorResume(ex -> Mono.empty());
    }

    /**
     * Hands the quarantined file to the external content-analysis service via the outbox.
     * The service fetches the binary from {@code storagePath} and replies asynchronously with a
     * {@code FILE_ANALYSIS_COMPLETED} event consumed by {@code FileAnalysisVerdictConsumer}.
     */
    private Mono<Void> requestAnalysis(StoredFile saved) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("fileId", saved.id().toString());
        payload.put("storagePath", saved.storagePath());
        payload.put("fileName", saved.fileName());
        payload.put("contentType", saved.contentType());
        payload.put("size", saved.size());
        payload.put("documentType", saved.documentType() == null ? "" : saved.documentType());
        return businessEventPublisher.publish(BusinessEvent.now(
                        saved.tenantId(), saved.organizationId(),
                        FileBusinessEventType.FILE_ANALYSIS_REQUESTED, FileBusinessEventType.AGGREGATE_TYPE, saved.id(),
                        payload))
                .onErrorResume(ex -> Mono.empty());
    }

    @Override
    public Mono<StoredFile> getMetadata(UUID fileId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> storedFileRepository.findById(context.tenantId(), fileId))
                .switchIfEmpty(Mono.error(new StoredFileNotFoundException(fileId)));
    }

    @Override
    public Mono<Resource> loadResource(UUID fileId) {
        return getMetadata(fileId)
                .flatMap(metadata -> {
                    if (!metadata.isAccepted()) {
                        return Mono.error(new StoredFileNotAvailableException(fileId, metadata.analysisStatus()));
                    }
                    return fileBinaryStorage.read(metadata.storagePath());
                });
    }

    /** Loads the binary regardless of analysis status — for admin review of quarantined files. */
    public Mono<Resource> loadResourceForReview(UUID fileId) {
        return getMetadata(fileId)
                .flatMap(metadata -> fileBinaryStorage.read(metadata.storagePath()));
    }

    @Override
    public Mono<StoredFile> applyVerdict(UUID tenantId, UUID fileId, FileAnalysisStatus verdict, String reason) {
        if (verdict != FileAnalysisStatus.ACCEPTED && verdict != FileAnalysisStatus.REJECTED) {
            return Mono.error(new InvalidStoredFileException("Analysis verdict must be ACCEPTED or REJECTED."));
        }
        return storedFileRepository.findById(tenantId, fileId)
                .switchIfEmpty(Mono.error(new StoredFileNotFoundException(fileId)))
                .flatMap(file -> {
                    StoredFile updated = verdict == FileAnalysisStatus.ACCEPTED
                            ? file.markAccepted()
                            : file.markRejected(reason);
                    return storedFileRepository.save(updated)
                            .flatMap(saved -> publishVerdict(saved).thenReturn(saved));
                });
    }

    private Mono<Void> publishVerdict(StoredFile saved) {
        String eventType = saved.isAccepted()
                ? FileBusinessEventType.FILE_ANALYSIS_ACCEPTED
                : FileBusinessEventType.FILE_ANALYSIS_REJECTED;
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("fileId", saved.id().toString());
        payload.put("analysisStatus", saved.analysisStatus().name());
        payload.put("reason", saved.analysisReason() == null ? "" : saved.analysisReason());
        return recordSystemAuditUseCase.record(saved.tenantId(), saved.organizationId(), saved.uploadedByUserId(),
                        eventType, FileBusinessEventType.AGGREGATE_TYPE, saved.id().toString(), saved.fileName())
                .then(businessEventPublisher.publish(BusinessEvent.now(saved.tenantId(), saved.organizationId(),
                        eventType, FileBusinessEventType.AGGREGATE_TYPE, saved.id(), payload)))
                .onErrorResume(ex -> Mono.empty());
    }

    private void validate(StoreFileCommand command, String normalizedFileName) {
        if (normalizedFileName.isBlank()) {
            throw new InvalidStoredFileException("File name is required.");
        }
        if (command.contentType() == null || command.contentType().isBlank()) {
            throw new InvalidStoredFileException("Content type is required.");
        }
        if (command.size() > 0 && command.size() > fileStorageProperties.getMaxFileSizeBytes()) {
            throw new InvalidStoredFileException(
                    "File size exceeds configured limit: " + fileStorageProperties.getMaxFileSizeBytes());
        }
        if (!fileStorageProperties.getAllowedContentTypes().isEmpty()) {
            String contentType = command.contentType().toLowerCase(Locale.ROOT);
            boolean allowed = fileStorageProperties.getAllowedContentTypes().stream()
                    .map(type -> type.toLowerCase(Locale.ROOT))
                    .anyMatch(contentType::equals);
            if (!allowed) {
                throw new InvalidStoredFileException("Content type is not allowed: " + command.contentType());
            }
        }
    }

    private String normalizeFileName(String rawFileName) {
        if (rawFileName == null || rawFileName.isBlank()) {
            return "";
        }
        String fileName = Path.of(rawFileName).getFileName().toString().trim();
        return fileName.replaceAll("[\\r\\n\\t]", "_");
    }
}
