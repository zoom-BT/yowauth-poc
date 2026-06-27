package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.kernel.application.port.in.SaveClientApplicationPlanCommand;
import java.util.List;

public record SaveClientApplicationPlanRequest(
        String code,
        String displayName,
        String description,
        List<String> allowedServices) {

    SaveClientApplicationPlanCommand toCommand(String fallbackCode) {
        return new SaveClientApplicationPlanCommand(
                code == null || code.isBlank() ? fallbackCode : code,
                displayName,
                description,
                allowedServices);
    }
}
