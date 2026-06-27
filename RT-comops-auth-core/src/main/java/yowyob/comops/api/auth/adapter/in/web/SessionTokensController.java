package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.application.port.out.UserAccountRepository;
import yowyob.comops.api.auth.application.service.RefreshTokenService;
import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.application.port.out.BusinessEventPublisher;
import yowyob.comops.api.kernel.application.port.out.ReactivePermissionResolver;
import yowyob.comops.api.kernel.application.service.ReactiveRequestContextHolder;
import yowyob.comops.api.kernel.config.JwtClaims;
import yowyob.comops.api.kernel.config.UserSessionTokenService;
import yowyob.comops.api.kernel.domain.model.BusinessEvent;

@RestController
@RequestMapping("/api/auth")
public class SessionTokensController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RefreshTokenService refreshTokenService;
    private final UserSessionTokenService userSessionTokenService;
    private final UserAccountRepository userAccountRepository;
    private final ReactivePermissionResolver permissionResolver;
    private final BusinessEventPublisher businessEventPublisher;

    public SessionTokensController(
            RefreshTokenService refreshTokenService,
            UserSessionTokenService userSessionTokenService,
            UserAccountRepository userAccountRepository,
            ReactivePermissionResolver permissionResolver,
            BusinessEventPublisher businessEventPublisher) {
        this.refreshTokenService = refreshTokenService;
        this.userSessionTokenService = userSessionTokenService;
        this.userAccountRepository = userAccountRepository;
        this.permissionResolver = permissionResolver;
        this.businessEventPublisher = businessEventPublisher;
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse<RefreshTokenResponse>>> refresh(
            @Valid @RequestBody Mono<RefreshTokenRequest> requestMono) {
        return Mono.zip(requestMono, ReactiveRequestContextHolder.getCorrelation())
                .flatMap(tuple -> {
                    String rawToken = tuple.getT1().refreshToken();
                    String remoteIp = tuple.getT2().map(c -> c.remoteIp()).orElse(null);
                    return refreshTokenService.rotate(rawToken, remoteIp, null)
                            .flatMap(issued -> userAccountRepository
                                    .findById(issued.token().tenantId(), issued.token().userId())
                                    .switchIfEmpty(Mono.error(new RefreshTokenService.InvalidRefreshTokenException(
                                            "User no longer exists.")))
                                    .flatMap(userAccount -> permissionResolver
                                            .resolvePermissions(userAccount.tenantId(), userAccount.id())
                                            .defaultIfEmpty(Set.of())
                                            .map(authorities -> {
                                                boolean isAdmin = authorities.stream()
                                                        .anyMatch(this::isPrivilegedAdminAuthority);
                                                String accessToken = userSessionTokenService.issueEnriched(
                                                        userAccount.tenantId(), null, null,
                                                        userAccount.id(), userAccount.actorId(), authorities,
                                                        userAccount.mfaEnabled(), isAdmin);
                                                return new RefreshTokenResponse(
                                                        accessToken,
                                                        issued.rawValue(),
                                                        "Bearer",
                                                        userSessionTokenService.getAccessTokenTtl().getSeconds(),
                                                        issued.ttlSeconds(),
                                                        issued.token().expiresAt());
                                            })));
                })
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Tokens refreshed.")));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<ApiResponse<Void>>> logout(
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorization,
            @RequestBody(required = false) Mono<RefreshTokenRequest> requestMono) {
        Mono<Void> revokeAccess = Mono.justOrEmpty(extractBearer(authorization))
                .flatMap(token -> Mono.justOrEmpty(userSessionTokenService.decodeIncludingRevoked(token)))
                .flatMap(claims -> userSessionTokenService.denyList()
                        .revoke(claims.jwtId(), claims.expiresAt())
                        .then(businessEventPublisher.publish(BusinessEvent.now(
                                claims.tenantId(), null,
                                "USER_LOGGED_OUT", "USER_ACCOUNT", claims.userId(),
                                Map.of("userId", claims.userId())))
                                .onErrorResume(e -> Mono.empty())))
                .onErrorResume(e -> Mono.empty())
                .then();

        Mono<Void> revokeRefresh = (requestMono == null ? Mono.<RefreshTokenRequest>empty() : requestMono)
                .filter(r -> r != null && r.refreshToken() != null && !r.refreshToken().isBlank())
                .flatMap(r -> refreshTokenService.rotate(r.refreshToken(), null, null)
                        .flatMap(issued -> refreshTokenService.revokeAllForUser(
                                issued.token().tenantId(), issued.token().userId()))
                        .onErrorResume(e -> Mono.empty()))
                .then();

        return revokeAccess.then(revokeRefresh)
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ApiResponse.<Void>success(null, "Logged out.")));
    }

    private boolean isPrivilegedAdminAuthority(String authority) {
        if (authority == null || authority.isBlank()) {
            return false;
        }
        String normalized = authority.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("ROLE_SYSTEM_ADMIN")
                || normalized.equals("ROLE_TENANT_ADMIN")
                || normalized.equals("ROLE_GENERAL_ADMIN")
                || normalized.equals("ROLE_IAM_ADMIN")
                || normalized.startsWith("ROLE_SYSTEM_ADMIN#")
                || normalized.startsWith("ROLE_TENANT_ADMIN#")
                || normalized.startsWith("ROLE_GENERAL_ADMIN#")
                || normalized.startsWith("ROLE_IAM_ADMIN#");
    }

    private Optional<String> extractBearer(String header) {
        if (header == null || header.isBlank() || !header.regionMatches(true, 0, BEARER_PREFIX, 0,
                BEARER_PREFIX.length())) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER_PREFIX.length()).trim());
    }
}
