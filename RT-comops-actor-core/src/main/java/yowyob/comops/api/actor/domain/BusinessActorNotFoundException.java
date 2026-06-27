package yowyob.comops.api.actor.domain;

import java.util.UUID;

public class BusinessActorNotFoundException extends RuntimeException {

    public BusinessActorNotFoundException(UUID actorId) {
        super("Business actor profile was not found for actor " + actorId + ".");
    }
}
