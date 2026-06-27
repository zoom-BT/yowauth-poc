package yowyob.comops.api.auth.domain;

public class InvalidLoginCredentialsException extends RuntimeException {

    public InvalidLoginCredentialsException() {
        super("Invalid login credentials.");
    }
}
