package yowyob.comops.api.actor.domain;

public class BusinessActorSelfReactivationDisabledException extends RuntimeException {

    public BusinessActorSelfReactivationDisabledException() {
        super("Business actor self-reactivation is disabled by platform options.");
    }
}
