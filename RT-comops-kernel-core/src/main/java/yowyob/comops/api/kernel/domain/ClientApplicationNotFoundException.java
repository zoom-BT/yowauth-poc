package yowyob.comops.api.kernel.domain;

import java.util.UUID;

public final class ClientApplicationNotFoundException extends RuntimeException {

    public ClientApplicationNotFoundException(UUID clientApplicationId) {
        super("Client application not found: " + clientApplicationId);
    }
}
