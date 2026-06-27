package yowyob.comops.api.file.domain;

import yowyob.comops.api.common.domain.DomainException;

public final class InvalidStoredFileException extends DomainException {
    public InvalidStoredFileException(String message) {
        super(message);
    }
}
