package yowyob.comops.api.kernel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.common.domain.model.ApiResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-5)
public class AdminMfaEnforcementWebFilter implements WebFilter {

    private final UserSessionTokenService userSessionTokenService;
    private final ObjectMapper objectMapper;

    public AdminMfaEnforcementWebFilter(UserSessionTokenService userSessionTokenService, ObjectMapper objectMapper) {
        this.userSessionTokenService = userSessionTokenService;
        this.objectMapper = objectMapper;
    }

    // Routes d'auto-enrôlement MFA accessibles à un admin privilégié SANS MFA, pour qu'il puisse
    // se débloquer lui-même (sinon deadlock : impossible d'activer la MFA car tout /api/ est bloqué).
    private static final java.util.Set<String> MFA_SELF_ENROLLMENT_PATHS = java.util.Set.of(
            "/api/auth/mfa/enable",
            "/api/auth/mfa/confirm",
            "/api/auth/logout",
            "/api/users/me");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }
        // Un admin privilégié sans MFA peut atteindre uniquement l'auto-enrôlement MFA.
        if (MFA_SELF_ENROLLMENT_PATHS.contains(path)) {
            return chain.filter(exchange);
        }
        String token = auth.substring(7);
        // Decode is synchronous (Optional). Évaluer le verdict d'abord, puis invoquer la chaîne
        // EXACTEMENT une fois. Ne jamais mettre chain.filter() dans un flatMap dont le résultat
        // (Mono<Void> vide) alimenterait un switchIfEmpty : cela ré-exécuterait toute la chaîne.
        boolean mfaBlocked = userSessionTokenService.decodeIncludingRevoked(token)
                .map(claims -> claims.privilegedAdmin() && !claims.mfaEnabled())
                .orElse(false);
        if (mfaBlocked) {
            return rejectMfaRequired(exchange);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> rejectMfaRequired(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(
                    ApiResponse.failure("MFA is required for admin accounts. Please enable MFA to access privileged resources.", "MFA_REQUIRED_FOR_ADMIN"));
        } catch (Exception e) {
            payload = "{\"success\":false,\"errorCode\":\"MFA_REQUIRED_FOR_ADMIN\"}".getBytes(StandardCharsets.UTF_8);
        }
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }
}
