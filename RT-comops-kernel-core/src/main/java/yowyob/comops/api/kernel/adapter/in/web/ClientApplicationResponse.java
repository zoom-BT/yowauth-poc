package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import yowyob.comops.api.kernel.domain.model.ClientApplication;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClientApplicationResponse(
        UUID id,
        String clientId,
        String name,
        String description,
        String status,
        boolean systemManaged,
        List<String> allowedServices,
        Instant createdAt,
        Instant updatedAt,
        Instant lastAuthenticatedAt,
        Instant secretRotatedAt) {

    public static ClientApplicationResponse from(ClientApplication clientApplication) {
        return new ClientApplicationResponse(
                clientApplication.id(),
                clientApplication.clientId(),
                clientApplication.name(),
                clientApplication.description(),
                clientApplication.status().name(),
                clientApplication.systemManaged(),
                PlatformServiceCode.orderCodes(clientApplication.allowedServiceCodes()),
                clientApplication.createdAt(),
                clientApplication.updatedAt(),
                clientApplication.lastAuthenticatedAt(),
                clientApplication.secretRotatedAt());
    }
}
