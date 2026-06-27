package yowyob.comops.api.auth.domain;

import yowyob.comops.api.common.domain.DomainException;

public final class DuplicateUsernameException extends DomainException {

    public DuplicateUsernameException(String username) {
        super("A user already exists with username: " + username);
    }
}
