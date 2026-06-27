package yowyob.comops.api.kernel.config;

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public final class ManagementApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;

    private ManagementApiKeyAuthenticationToken(String apiKey,
            Collection<? extends GrantedAuthority> authorities,
            boolean authenticated) {
        super(authorities);
        this.apiKey = apiKey;
        setAuthenticated(authenticated);
    }

    public static ManagementApiKeyAuthenticationToken unauthenticated(String apiKey) {
        return new ManagementApiKeyAuthenticationToken(apiKey, List.of(), false);
    }

    public static ManagementApiKeyAuthenticationToken authenticated(String apiKey,
            Collection<? extends GrantedAuthority> authorities) {
        return new ManagementApiKeyAuthenticationToken(apiKey, authorities, true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return "management-api-key";
    }
}
