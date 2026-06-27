package yowyob.comops.api.actor.domain;

import java.util.UUID;

public class UserAccountLinkNotFoundException extends RuntimeException {

    public UserAccountLinkNotFoundException(UUID userId) {
        super("User account " + userId + " is not linked to an actor.");
    }
}
