package yowyob.comops.api.auth.adapter.in.web;

import yowyob.comops.api.auth.application.port.in.DiscoverLoginContextsResult;
import java.util.List;

public record DiscoverLoginContextsResponse(
        String selectionToken,
        long expiresInSeconds,
        List<DiscoveredLoginContextResponse> contexts) {

    public static DiscoverLoginContextsResponse from(DiscoverLoginContextsResult result) {
        return new DiscoverLoginContextsResponse(
                result.selectionToken(),
                result.expiresInSeconds(),
                result.contexts().stream()
                        .map(DiscoveredLoginContextResponse::from)
                        .toList());
    }
}
