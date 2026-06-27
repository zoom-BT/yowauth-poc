package yowyob.comops.api.auth.domain;

import yowyob.comops.api.common.domain.DomainException;

public final class DuplicateEmailException extends DomainException {

    public DuplicateEmailException(String email) {
        super("A user already exists with email: " + email);
    }
}
