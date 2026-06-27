package yowyob.comops.api.kernel.application.port.in;

import java.util.UUID;

public record RotateClientApplicationSecretCommand(UUID clientApplicationId, String clientSecret) {
}
