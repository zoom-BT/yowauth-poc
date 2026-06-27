package yowyob.comops.api.file.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoredFileTest {

    private StoredFile newFile() {
        return StoredFile.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "invoice.pdf",
                "application/pdf", 1024, "tenant/abc/invoice.pdf", "ID_CARD");
    }

    @Test
    void createStartsPendingAndNotDownloadable() {
        StoredFile file = newFile();
        assertThat(file.analysisStatus()).isEqualTo(FileAnalysisStatus.PENDING);
        assertThat(file.isAccepted()).isFalse();
        assertThat(file.analyzedAt()).isNull();
    }

    @Test
    void markAcceptedClearsForDownload() {
        StoredFile accepted = newFile().markAccepted();
        assertThat(accepted.analysisStatus()).isEqualTo(FileAnalysisStatus.ACCEPTED);
        assertThat(accepted.isAccepted()).isTrue();
        assertThat(accepted.analyzedAt()).isNotNull();
        assertThat(accepted.analysisReason()).isNull();
    }

    @Test
    void markRejectedKeepsReasonAndBlocksDownload() {
        StoredFile rejected = newFile().markRejected("malware detected");
        assertThat(rejected.analysisStatus()).isEqualTo(FileAnalysisStatus.REJECTED);
        assertThat(rejected.isAccepted()).isFalse();
        assertThat(rejected.analysisReason()).isEqualTo("malware detected");
        assertThat(rejected.analyzedAt()).isNotNull();
    }
}
