package yowyob.comops.api.common.domain;

/**
 * Base exception type for domain and application errors.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
