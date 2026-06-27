package yowyob.comops.api.file.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import yowyob.comops.api.file.application.port.out.FileBinaryStorage;
import yowyob.comops.api.file.application.port.out.StoredFileRepository;
import yowyob.comops.api.file.config.FileStorageProperties;
import yowyob.comops.api.file.domain.InvalidStoredFileException;
import yowyob.comops.api.file.domain.model.FileAnalysisStatus;
import yowyob.comops.api.file.domain.model.StoredFile;
import yowyob.comops.api.kernel.application.port.in.RecordSystemAuditUseCase;
import yowyob.comops.api.kernel.application.port.out.BusinessEventPublisher;

@ExtendWith(MockitoExtension.class)
class FileApplicationServiceVerdictTest {

    @Mock
    private StoredFileRepository storedFileRepository;
    @Mock
    private FileBinaryStorage fileBinaryStorage;
    @Mock
    private RecordSystemAuditUseCase recordSystemAuditUseCase;
    @Mock
    private BusinessEventPublisher businessEventPublisher;

    private FileApplicationService service;

    @BeforeEach
    void setUp() {
        service = new FileApplicationService(storedFileRepository, fileBinaryStorage, recordSystemAuditUseCase,
                businessEventPublisher, new FileStorageProperties());
        lenient().when(storedFileRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        lenient().when(recordSystemAuditUseCase.record(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
        lenient().when(businessEventPublisher.publish(any())).thenReturn(Mono.empty());
    }

    private StoredFile pendingFile(UUID tenantId, UUID fileId) {
        return StoredFile.rehydrate(fileId, tenantId, java.time.Instant.now(), java.time.Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "doc.pdf", "application/pdf", 10, "p/doc.pdf", "ID_CARD",
                FileAnalysisStatus.PENDING, null, null);
    }

    @Test
    void acceptedVerdictClearsFile() {
        UUID tenantId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        when(storedFileRepository.findById(tenantId, fileId)).thenReturn(Mono.just(pendingFile(tenantId, fileId)));

        StepVerifier.create(service.applyVerdict(tenantId, fileId, FileAnalysisStatus.ACCEPTED, null))
                .expectNextMatches(StoredFile::isAccepted)
                .verifyComplete();
    }

    @Test
    void rejectedVerdictQuarantinesWithReason() {
        UUID tenantId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        when(storedFileRepository.findById(tenantId, fileId)).thenReturn(Mono.just(pendingFile(tenantId, fileId)));

        StepVerifier.create(service.applyVerdict(tenantId, fileId, FileAnalysisStatus.REJECTED, "virus"))
                .expectNextMatches(file -> file.analysisStatus() == FileAnalysisStatus.REJECTED
                        && "virus".equals(file.analysisReason()))
                .verifyComplete();
    }

    @Test
    void pendingVerdictIsRejectedAsInvalid() {
        StepVerifier.create(service.applyVerdict(UUID.randomUUID(), UUID.randomUUID(),
                        FileAnalysisStatus.PENDING, null))
                .expectError(InvalidStoredFileException.class)
                .verify();
    }
}
