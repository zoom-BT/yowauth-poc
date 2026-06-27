package yowyob.comops.api.kernel.domain.model;

import java.time.Instant;

public record RequestCorrelation(
        String requestId,
        String clientApplicationId,
        String remoteIp,
        String httpMethod,
        String requestPath,
        Instant receivedAt) {
}
