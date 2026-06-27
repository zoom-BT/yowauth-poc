package yowyob.comops.api.kernel.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.config.ApiKeyAuthenticationToken;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public final class ClientApplicationServiceEntitlementWebFilter implements WebFilter {

    private final PlatformServiceRouteResolver routeResolver;
    private final ObjectMapper objectMapper;

    public ClientApplicationServiceEntitlementWebFilter(PlatformServiceRouteResolver routeResolver,
            ObjectMapper objectMapper) {
        this.routeResolver = routeResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String serviceCode = routeResolver.resolveClientApplicationServiceCode(
                exchange.getRequest().getPath().pathWithinApplication().value());
        if (serviceCode == null) {
            return chain.filter(exchange);
        }
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(securityContext -> securityContext.getAuthentication())
                .mapNotNull(authentication -> authentication instanceof ApiKeyAuthenticationToken token ? token : null)
                .flatMap(token -> token.allowedServiceCodes().contains(serviceCode)
                        ? chain.filter(exchange).thenReturn(Boolean.TRUE)
                        : writeFailure(exchange, HttpStatus.FORBIDDEN,
                                "Client application is not allowed to access service " + serviceCode + ".",
                                "CLIENT_APPLICATION_SERVICE_NOT_ALLOWED").thenReturn(Boolean.TRUE))
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange).thenReturn(Boolean.TRUE)))
                .then();
    }

    private Mono<Void> writeFailure(ServerWebExchange exchange, HttpStatus status, String message, String errorCode) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(ApiResponse.failure(message, errorCode));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            payload = ("{\"success\":false,\"message\":\"" + message.replace("\"", "'")
                    + "\",\"errorCode\":\"" + errorCode + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }
}
