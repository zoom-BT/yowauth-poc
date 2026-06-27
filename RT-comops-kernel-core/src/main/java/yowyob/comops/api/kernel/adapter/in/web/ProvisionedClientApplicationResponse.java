package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.kernel.application.service.ProvisionedClientApplication;

public record ProvisionedClientApplicationResponse(
        ClientApplicationResponse clientApplication,
        String clientSecret) {

    public static ProvisionedClientApplicationResponse from(ProvisionedClientApplication provisioned) {
        return new ProvisionedClientApplicationResponse(
                ClientApplicationResponse.from(provisioned.clientApplication()),
                provisioned.clientSecret());
    }
}
