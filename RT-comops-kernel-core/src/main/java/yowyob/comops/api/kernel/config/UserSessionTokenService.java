package yowyob.comops.api.kernel.config;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserSessionTokenService {

    private final JwtTokenService jwtTokenService;
    private final JwtDenyListService denyListService;

    public UserSessionTokenService(JwtTokenService jwtTokenService, JwtDenyListService denyListService) {
        this.jwtTokenService = jwtTokenService;
        this.denyListService = denyListService;
    }

    public String issue(UUID tenantId, UUID userId, UUID actorId) {
        return issue(tenantId, null, null, userId, actorId, Set.of());
    }

    public String issue(UUID tenantId, UUID userId, UUID actorId, Set<String> permissions) {
        return issue(tenantId, null, null, userId, actorId, permissions);
    }

    public String issue(
            UUID tenantId,
            UUID organizationId,
            UUID agencyId,
            UUID userId,
            UUID actorId,
            Set<String> permissions) {
        return jwtTokenService.issueAccessToken(tenantId, organizationId, agencyId, userId, actorId, permissions);
    }

    public String issueEnriched(
            UUID tenantId,
            UUID organizationId,
            UUID agencyId,
            UUID userId,
            UUID actorId,
            Set<String> permissions,
            boolean mfaEnabled,
            boolean privilegedAdmin) {
        return jwtTokenService.issueAccessToken(tenantId, organizationId, agencyId, userId, actorId, permissions,
                mfaEnabled, privilegedAdmin);
    }

    public Optional<UserSessionTokenClaims> verify(String token) {
        try {
            return jwtTokenService.decode(token)
                    .filter(claims -> !denyListService.isRevoked(claims.jwtId()))
                    .map(UserSessionTokenClaims::fromJwtClaims);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public Optional<JwtClaims> decodeIncludingRevoked(String token) {
        try {
            return jwtTokenService.decode(token);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public boolean isJwtEnabled() {
        return true;
    }

    public Duration getAccessTokenTtl() {
        return jwtTokenService.getAccessTokenTtl();
    }

    public JwtDenyListService denyList() {
        return denyListService;
    }
}
