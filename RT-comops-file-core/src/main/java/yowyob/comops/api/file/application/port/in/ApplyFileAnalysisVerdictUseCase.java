package yowyob.comops.api.file.application.port.in;

import java.util.UUID;
import reactor.core.publisher.Mono;
import yowyob.comops.api.file.domain.model.FileAnalysisStatus;
import yowyob.comops.api.file.domain.model.StoredFile;

/**
 * Applies the verdict produced by the external content-analysis service to a quarantined file.
 * {@code verdict} must be {@link FileAnalysisStatus#ACCEPTED} or {@link FileAnalysisStatus#REJECTED}.
 */
public interface ApplyFileAnalysisVerdictUseCase {
    Mono<StoredFile> applyVerdict(UUID tenantId, UUID fileId, FileAnalysisStatus verdict, String reason);
}
