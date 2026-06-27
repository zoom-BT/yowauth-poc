package yowyob.comops.api.auth.adapter.in.web;

import java.time.Instant;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds,
        Instant refreshExpiresAt) {
}
