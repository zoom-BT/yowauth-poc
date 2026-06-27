package yowyob.comops.api.file.domain.model;

/**
 * Business event types emitted/consumed by the file module.
 *
 * <p>Integration contract with the external content-analysis service:
 * <ul>
 *   <li>file-core emits {@link #FILE_ANALYSIS_REQUESTED} (aggregateType {@code STORED_FILE},
 *       aggregateId = file id) with payload {@code fileId, storagePath, fileName, contentType, size}
 *       once a file is persisted in quarantine.</li>
 *   <li>the external service analyses the file and emits {@link #FILE_ANALYSIS_COMPLETED}
 *       (same aggregate) with payload {@code fileId, verdict} where verdict is
 *       {@code ACCEPTED} or {@code REJECTED}, plus an optional {@code reason}.</li>
 * </ul>
 */
public final class FileBusinessEventType {

    public static final String FILE_UPLOADED = "FILE_UPLOADED";
    public static final String FILE_ANALYSIS_REQUESTED = "FILE_ANALYSIS_REQUESTED";
    public static final String FILE_ANALYSIS_COMPLETED = "FILE_ANALYSIS_COMPLETED";
    public static final String FILE_ANALYSIS_ACCEPTED = "FILE_ANALYSIS_ACCEPTED";
    public static final String FILE_ANALYSIS_REJECTED = "FILE_ANALYSIS_REJECTED";

    public static final String AGGREGATE_TYPE = "STORED_FILE";

    private FileBusinessEventType() {
    }
}
