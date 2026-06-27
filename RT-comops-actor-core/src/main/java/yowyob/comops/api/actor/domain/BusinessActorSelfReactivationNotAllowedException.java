package yowyob.comops.api.actor.domain;

import java.util.UUID;

public class BusinessActorSelfReactivationNotAllowedException extends RuntimeException {

    public BusinessActorSelfReactivationNotAllowedException(UUID businessActorId, String status) {
        super("Business actor " + businessActorId + " cannot self-reactivate from status " + status + ".");
    }
}
