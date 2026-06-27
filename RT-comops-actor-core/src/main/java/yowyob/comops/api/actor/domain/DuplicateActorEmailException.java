package yowyob.comops.api.actor.domain;

import yowyob.comops.api.common.domain.DomainException;

public class DuplicateActorEmailException extends DomainException {

    public DuplicateActorEmailException(String email) {
        super("An actor already exists with email: " + email);
    }
}
