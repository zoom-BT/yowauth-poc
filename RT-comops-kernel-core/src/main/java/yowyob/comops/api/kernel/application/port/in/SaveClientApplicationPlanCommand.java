package yowyob.comops.api.kernel.application.port.in;

import java.util.List;

public record SaveClientApplicationPlanCommand(
        String code,
        String displayName,
        String description,
        List<String> allowedServices) {
}
