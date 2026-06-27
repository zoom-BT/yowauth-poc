package yowyob.comops.api.auth.application.port.in;

import java.util.List;

public record DiscoverSignUpContextsResult(
        String selectionToken,
        long expiresInSeconds,
        List<SelectableSignUpContext> contexts) {
}
