package yowyob.comops.api.auth.adapter.in.web;

import java.util.List;
import yowyob.comops.api.auth.application.port.in.DiscoverSignUpContextsResult;

public record DiscoverSignUpContextsResponse(
        String selectionToken,
        long expiresInSeconds,
        List<SelectableSignUpContextResponse> contexts) {

    public static DiscoverSignUpContextsResponse from(DiscoverSignUpContextsResult result) {
        return new DiscoverSignUpContextsResponse(
                result.selectionToken(),
                result.expiresInSeconds(),
                result.contexts().stream().map(SelectableSignUpContextResponse::from).toList());
    }
}
