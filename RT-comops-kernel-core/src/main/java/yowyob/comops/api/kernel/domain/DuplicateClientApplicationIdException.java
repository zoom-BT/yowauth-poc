package yowyob.comops.api.kernel.domain;

public final class DuplicateClientApplicationIdException extends RuntimeException {

    public DuplicateClientApplicationIdException(String clientId) {
        super("Client application already exists: " + clientId);
    }
}
