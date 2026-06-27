package yowyob.comops.api.auth.application.port.in;

import java.util.List;

public record DiscoverLoginContextsResult(
        String selectionToken,
        long expiresInSeconds,
        List<SelectableLoginContext> contexts) {
}
