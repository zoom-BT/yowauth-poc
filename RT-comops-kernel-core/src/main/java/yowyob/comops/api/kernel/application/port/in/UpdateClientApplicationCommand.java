package yowyob.comops.api.kernel.application.port.in;

import java.util.List;
import java.util.UUID;

public record UpdateClientApplicationCommand(
        UUID clientApplicationId,
        String name,
        String description,
        String planCode,
        List<String> allowedServices) {
}
