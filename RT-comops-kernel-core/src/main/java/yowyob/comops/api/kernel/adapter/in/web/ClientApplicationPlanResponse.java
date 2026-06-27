package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.kernel.application.port.in.ClientApplicationPlanView;
import java.util.List;

public record ClientApplicationPlanResponse(
        String code,
        String displayName,
        String description,
        List<String> allowedServices,
        boolean systemDefault) {

    static ClientApplicationPlanResponse from(ClientApplicationPlanView view) {
        return new ClientApplicationPlanResponse(view.code(), view.displayName(), view.description(),
                view.allowedServices(), view.systemDefault());
    }
}
