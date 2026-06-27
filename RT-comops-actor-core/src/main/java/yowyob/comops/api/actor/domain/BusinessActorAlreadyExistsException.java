package yowyob.comops.api.actor.domain;

import java.util.UUID;

public class BusinessActorAlreadyExistsException extends RuntimeException {

    public BusinessActorAlreadyExistsException(UUID actorId) {
        super("Business actor profile already exists for actor " + actorId + ".");
    }
}
