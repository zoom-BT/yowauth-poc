package yowyob.comops.api.kernel.config;

import yowyob.comops.api.kernel.application.port.in.AuthenticateClientApplicationUseCase;
import yowyob.comops.api.kernel.application.port.out.ReactivePermissionResolver;
import yowyob.comops.api.kernel.domain.model.ClientApplication;
import java.util.stream.Collectors;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import reactor.core.publisher.Mono;

public final class ApiKeyReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final AuthenticateClientApplicationUseCase authenticateClientApplicationUseCase;
    private final ReactivePermissionResolver permissionResolver;

    public ApiKeyReactiveAuthenticationManager(AuthenticateClientApplicationUseCase authenticateClientApplicationUseCase,
            ReactivePermissionResolver permissionResolver) {
        this.authenticateClientApplicationUseCase = authenticateClientApplicationUseCase;
        this.permissionResolver = permissionResolver;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return Mono.error(new BadCredentialsException("Missing API key"));
        }
        if (!(authentication instanceof ApiKeyAuthenticationToken token)) {
            return Mono.error(new BadCredentialsException("Missing client application context"));
        }
        String providedApiKey = authentication.getCredentials().toString();
        if (token.clientId() == null || token.clientId().isBlank()) {
            return Mono.error(new BadCredentialsException("Missing client application id"));
        }
        return authenticateClientApplicationUseCase.authenticate(token.clientId(), providedApiKey)
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid client application credentials")))
                .flatMap(clientApplication -> authenticateUserContext(token, providedApiKey, clientApplication));
    }

    private Mono<Authentication> authenticateUserContext(
            ApiKeyAuthenticationToken token,
            String providedApiKey,
            ClientApplication clientApplication) {
        if (token.tenantId() == null || token.userId() == null) {
            return Mono.just(ApiKeyAuthenticationToken.authenticated(clientApplication.id(), clientApplication.clientId(),
                    providedApiKey, token.tenantId(), token.organizationId(), token.agencyId(), token.userId(),
                    token.actorId(), clientApplication.allowedServiceCodes(), java.util.List.of()));
        }
        return permissionResolver.resolvePermissions(token.tenantId(), token.userId())
                .map(permissions -> permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet()))
                .map(authorities -> ApiKeyAuthenticationToken.authenticated(clientApplication.id(),
                        clientApplication.clientId(), providedApiKey, token.tenantId(), token.organizationId(),
                        token.agencyId(), token.userId(), token.actorId(), clientApplication.allowedServiceCodes(),
                        authorities));
    }
}
