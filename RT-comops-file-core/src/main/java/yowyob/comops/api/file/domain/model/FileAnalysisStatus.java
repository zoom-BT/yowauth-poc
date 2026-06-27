package yowyob.comops.api.file.domain.model;

/**
 * Lifecycle of a stored file relative to the external content-analysis service.
 *
 * <ul>
 *   <li>{@code PENDING} — uploaded and persisted in quarantine, awaiting the analysis verdict.</li>
 *   <li>{@code ACCEPTED} — cleared by the analysis service; the file is downloadable.</li>
 *   <li>{@code REJECTED} — flagged by the analysis service; the binary is kept in quarantine
 *       (admin review only) and normal download is blocked.</li>
 * </ul>
 */
public enum FileAnalysisStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
