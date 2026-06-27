package yowyob.comops.api.file.domain;

import yowyob.comops.api.common.domain.DomainException;
import java.util.UUID;
import yowyob.comops.api.file.domain.model.FileAnalysisStatus;

/**
 * Raised when a stored file is requested for normal download but has not been cleared by the
 * external content-analysis service (still {@code PENDING} or {@code REJECTED} / quarantined).
 */
public final class StoredFileNotAvailableException extends DomainException {
    public StoredFileNotAvailableException(UUID fileId, FileAnalysisStatus status) {
        super("Stored file " + fileId + " is not available for download (analysis status: " + status + ")");
    }
}
