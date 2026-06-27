package yowyob.comops.api.kernel.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public final class ManagementApiKeyAuthenticationConverter implements ServerAuthenticationConverter {

    public static final String MANAGEMENT_API_KEY_HEADER = "X-Management-Api-Key";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(MANAGEMENT_API_KEY_HEADER))
                .map(ManagementApiKeyAuthenticationToken::unauthenticated)
                .cast(Authentication.class);
    }
}
