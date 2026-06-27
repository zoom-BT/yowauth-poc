package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.kernel.domain.model.ClientApplication;

public record ProvisionedClientApplication(ClientApplication clientApplication, String clientSecret) {
}
