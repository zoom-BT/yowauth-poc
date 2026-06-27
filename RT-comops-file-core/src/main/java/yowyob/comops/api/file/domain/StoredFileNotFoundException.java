package yowyob.comops.api.file.domain;

import yowyob.comops.api.common.domain.DomainException;
import java.util.UUID;

public final class StoredFileNotFoundException extends DomainException {
    public StoredFileNotFoundException(UUID fileId) {
        super("Stored file not found: " + fileId);
    }
}
