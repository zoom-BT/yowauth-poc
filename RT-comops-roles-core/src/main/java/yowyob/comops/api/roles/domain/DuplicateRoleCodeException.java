package yowyob.comops.api.roles.domain;

import yowyob.comops.api.common.domain.DomainException;

public final class DuplicateRoleCodeException extends DomainException {

    public DuplicateRoleCodeException(String code) {
        super("A role already exists with code: " + code);
    }
}
