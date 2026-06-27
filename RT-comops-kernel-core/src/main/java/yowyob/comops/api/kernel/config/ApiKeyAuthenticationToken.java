package yowyob.comops.api.kernel.config;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public final class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final java.util.UUID clientApplicationId;
    private final String clientId;
    private final String apiKey;
    private final UUID tenantId;
    private final UUID organizationId;
    private final UUID agencyId;
    private final UUID userId;
    private final UUID actorId;
    private final Set<String> allowedServiceCodes;

    private ApiKeyAuthenticationToken(java.util.UUID clientApplicationId, String clientId, String apiKey, UUID tenantId,
            UUID organizationId, UUID agencyId, UUID userId, UUID actorId,
            Set<String> allowedServiceCodes,
            Collection<? extends GrantedAuthority> authorities, boolean authenticated) {
        super(authorities);
        this.clientApplicationId = clientApplicationId;
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.tenantId = tenantId;
        this.organizationId = organizationId;
        this.agencyId = agencyId;
        this.userId = userId;
        this.actorId = actorId;
        this.allowedServiceCodes = allowedServiceCodes == null ? Set.of() : Set.copyOf(allowedServiceCodes);
        setAuthenticated(authenticated);
    }

    public static ApiKeyAuthenticationToken unauthenticated(String clientId, String apiKey, UUID tenantId,
            UUID organizationId, UUID agencyId, UUID userId, UUID actorId) {
        return new ApiKeyAuthenticationToken(null, clientId, apiKey, tenantId, organizationId, agencyId, userId,
                actorId, Set.of(),
                java.util.List.of(), false);
    }

    public static ApiKeyAuthenticationToken authenticated(java.util.UUID clientApplicationId, String clientId,
            String apiKey, UUID tenantId, UUID organizationId, UUID agencyId, UUID userId, UUID actorId,
            Set<String> allowedServiceCodes,
            Collection<? extends GrantedAuthority> authorities) {
        return new ApiKeyAuthenticationToken(clientApplicationId, clientId, apiKey, tenantId, organizationId, agencyId,
                userId, actorId, allowedServiceCodes, authorities, true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return userId != null ? userId : clientId;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID userId() {
        return userId;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID agencyId() {
        return agencyId;
    }

    public UUID actorId() {
        return actorId;
    }

    public java.util.UUID clientApplicationId() {
        return clientApplicationId;
    }

    public String clientId() {
        return clientId;
    }

    public Set<String> allowedServiceCodes() {
        return allowedServiceCodes;
    }
}
