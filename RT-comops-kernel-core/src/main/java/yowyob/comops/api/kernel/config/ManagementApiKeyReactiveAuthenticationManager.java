package yowyob.comops.api.kernel.config;

import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

public final class ManagementApiKeyReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final String expectedApiKey;

    public ManagementApiKeyReactiveAuthenticationManager(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return Mono.error(new BadCredentialsException("Missing management API key"));
        }
        String providedApiKey = authentication.getCredentials().toString();
        if (!expectedApiKey.equals(providedApiKey)) {
            return Mono.error(new BadCredentialsException("Invalid management API key"));
        }
        return Mono.just(ManagementApiKeyAuthenticationToken.authenticated(
                providedApiKey,
                List.of(new SimpleGrantedAuthority("management:read"))));
    }
}
