package yowyob.comops.api.auth.application.service;

import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.application.port.out.UserAccountRepository;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccessDirectory;
import yowyob.comops.api.auth.domain.model.UserAccount;
import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import yowyob.comops.api.kernel.application.port.in.AuthenticateClientApplicationUseCase;
import yowyob.comops.api.kernel.application.port.in.RecordSystemAuditUseCase;
import yowyob.comops.api.kernel.application.port.out.ReactivePermissionResolver;
import yowyob.comops.api.kernel.config.JwtTokenService;
import yowyob.comops.api.kernel.domain.model.ClientApplication;

@Service
public class AuthOidcService {

    public static final String TOKEN_EXCHANGE_GRANT = "urn:ietf:params:oauth:grant-type:token-exchange";
    public static final String JWT_SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    public static final String ACCESS_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";
    private static final String SSO_SESSION_TOKEN_KIND = "auth-sso-session";

    private final AuthenticateClientApplicationUseCase authenticateClientApplicationUseCase;
    private final UserAccountRepository userAccountRepository;
    private final UserOrganizationAccessDirectory userOrganizationAccessDirectory;
    private final ReactivePermissionResolver permissionResolver;
    private final JwtTokenService jwtTokenService;
    private final AuthSsoSessionTokenService authSsoSessionTokenService;
    private final AuthSharedSessionService authSharedSessionService;
    private final RecordSystemAuditUseCase recordSystemAuditUseCase;

    public AuthOidcService(
            AuthenticateClientApplicationUseCase authenticateClientApplicationUseCase,
            UserAccountRepository userAccountRepository,
            UserOrganizationAccessDirectory userOrganizationAccessDirectory,
            ReactivePermissionResolver permissionResolver,
            JwtTokenService jwtTokenService,
            AuthSsoSessionTokenService authSsoSessionTokenService,
            AuthSharedSessionService authSharedSessionService,
            RecordSystemAuditUseCase recordSystemAuditUseCase) {
        this.authenticateClientApplicationUseCase = authenticateClientApplicationUseCase;
        this.userAccountRepository = userAccountRepository;
        this.userOrganizationAccessDirectory = userOrganizationAccessDirectory;
        this.permissionResolver = permissionResolver;
        this.jwtTokenService = jwtTokenService;
        this.authSsoSessionTokenService = authSsoSessionTokenService;
        this.authSharedSessionService = authSharedSessionService;
        this.recordSystemAuditUseCase = recordSystemAuditUseCase;
    }

    public Mono<IssuedAccessToken> exchangeToken(
            ClientAuthentication clientAuthentication,
            TokenExchangeRequest request) {
        requireEquals(request.grantType(), TOKEN_EXCHANGE_GRANT, OAuthException.invalidRequest("Unsupported grant_type."));
        requireEquals(request.subjectTokenType(), JWT_SUBJECT_TOKEN_TYPE,
                OAuthException.invalidRequest("Unsupported subject_token_type."));
        String normalizedContextId = requireText(request.contextId(), "context_id");
        String subjectToken = requireText(request.subjectToken(), "subject_token");
        PlatformServiceCode requestedService = PlatformServiceCode.from(requireText(request.serviceCode(), "service_code"));
        String requestedTokenType = normalizeNullableText(request.requestedTokenType());
        if (requestedTokenType != null && !ACCESS_TOKEN_TYPE.equals(requestedTokenType)) {
            throw OAuthException.invalidRequest("Unsupported requested_token_type.");
        }

        return authenticateClient(clientAuthentication)
                .flatMap(clientApplication -> {
                    if (!clientApplication.canAccessService(requestedService.code())) {
                        return Mono.error(OAuthException.invalidGrant(
                                "Client application is not allowed to access service " + requestedService.code() + "."));
                    }
                    return Mono.justOrEmpty(authSsoSessionTokenService.verify(subjectToken))
                            .switchIfEmpty(Mono.error(OAuthException.invalidGrant(
                                    "Invalid or expired SSO session token.")))
                            .map(verified -> verified.contexts().stream()
                                    .filter(context -> context.contextId().equals(normalizedContextId))
                                    .findFirst()
                                    .orElseThrow(() -> OAuthException.invalidGrant(
                                            "The selected SSO context is not available.")))
                            .flatMap(context -> issueClientAccessToken(
                                    clientApplication,
                                    requestedService,
                                    context.tenantId(),
                                    context.userId(),
                                    context.actorId(),
                                    request.organizationId(),
                                    request.agencyId(),
                                    request.scope()));
                });
    }

    public Mono<OidcUserInfoPayload> userInfo(String bearerToken) {
        String normalizedToken = requireText(bearerToken, "bearer token");
        if (authSsoSessionTokenService.verify(normalizedToken).isPresent()) {
            return authSharedSessionService.userInfo(normalizedToken)
                    .map(OidcUserInfoPayload::fromSharedSession);
        }
        return Mono.justOrEmpty(jwtTokenService.decodeSignedToken(normalizedToken))
                .switchIfEmpty(Mono.error(OAuthException.invalidToken("Invalid or expired bearer token.")))
                .flatMap(this::toAccessTokenUserInfo);
    }

    public Mono<Map<String, Object>> introspect(
            ClientAuthentication clientAuthentication,
            String token,
            String tokenTypeHint) {
        String normalizedToken = requireText(token, "token");
        String normalizedHint = normalizeNullableText(tokenTypeHint);
        return authenticateClient(clientAuthentication)
                .flatMap(clientApplication -> introspectToken(normalizedToken, normalizedHint, clientApplication));
    }

    public Map<String, Object> openIdConfiguration(
            String issuerBaseUrl,
            String tokenEndpoint,
            String userInfoEndpoint,
            String jwksUri,
            String introspectionEndpoint) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("issuer", jwtTokenService.getIssuer());
        payload.put("jwks_uri", jwksUri);
        payload.put("token_endpoint", tokenEndpoint);
        payload.put("userinfo_endpoint", userInfoEndpoint);
        payload.put("introspection_endpoint", introspectionEndpoint);
        payload.put("grant_types_supported", java.util.List.of(TOKEN_EXCHANGE_GRANT));
        payload.put("subject_token_types_supported", java.util.List.of(JWT_SUBJECT_TOKEN_TYPE));
        payload.put("token_endpoint_auth_methods_supported", java.util.List.of("client_secret_basic", "client_secret_post"));
        payload.put("introspection_endpoint_auth_methods_supported",
                java.util.List.of("client_secret_basic", "client_secret_post"));
        payload.put("scopes_supported", java.util.List.of("openid", "profile", "email"));
        payload.put("claims_supported", java.util.List.of(
                "sub", "sid", "preferredUsername", "email", "tenantId", "organizationId", "agencyId", "actorId",
                "permissions", "clientId", "serviceCode", "sso", "contexts"));
        payload.put("id_token_signing_alg_values_supported", java.util.List.of("RS256"));
        payload.put("service_documentation", issuerBaseUrl + "/swagger-ui.html");
        return payload;
    }

    private Mono<IssuedAccessToken> issueClientAccessToken(
            ClientApplication clientApplication,
            PlatformServiceCode requestedService,
            UUID tenantId,
            UUID userId,
            UUID actorId,
            UUID organizationId,
            UUID agencyId,
            String requestedScope) {
        return userAccountRepository.findById(tenantId, userId)
                .switchIfEmpty(Mono.error(OAuthException.invalidGrant(
                        "The selected SSO context is no longer available.")))
                .flatMap(userAccount -> validateOrganizationAccess(userAccount, organizationId)
                        .then(permissionResolver.resolvePermissions(tenantId, userId).defaultIfEmpty(Set.of()))
                        .flatMap(authorities -> recordSystemAuditUseCase.record(
                                        tenantId,
                                        organizationId,
                                        userId,
                                        "USER_SSO_TOKEN_EXCHANGED",
                                        "USER_ACCOUNT",
                                        userId.toString(),
                                        clientApplication.clientId() + ":" + requestedService.code())
                                .thenReturn(issueToken(
                                        clientApplication,
                                        requestedService,
                                        tenantId,
                                        organizationId,
                                        agencyId,
                                        userId,
                                        actorId,
                                        authorities,
                                        requestedScope))));
    }

    private IssuedAccessToken issueToken(
            ClientApplication clientApplication,
            PlatformServiceCode requestedService,
            UUID tenantId,
            UUID organizationId,
            UUID agencyId,
            UUID userId,
            UUID actorId,
            Set<String> authorities,
            String requestedScope) {
        ScopeGrant scopeGrant = resolveScopeGrant(requestedService, authorities, requestedScope);
        Map<String, Object> additionalClaims = new LinkedHashMap<>();
        additionalClaims.put("tid", tenantId.toString());
        additionalClaims.put("actor", actorId == null ? null : actorId.toString());
        additionalClaims.put("oid", organizationId == null ? null : organizationId.toString());
        additionalClaims.put("aid", agencyId == null ? null : agencyId.toString());
        additionalClaims.put("permissions", scopeGrant.permissions().stream().sorted().toList());
        additionalClaims.put("azp", clientApplication.clientId());
        additionalClaims.put("svc", requestedService.code());
        additionalClaims.put("sso", true);
        String token = jwtTokenService.issueSignedToken(
                userId.toString(),
                jwtTokenService.getAccessTokenTtl(),
                additionalClaims);
        Duration ttl = jwtTokenService.getAccessTokenTtl();
        return new IssuedAccessToken(
                token,
                "Bearer",
                ttl == null ? 0L : ttl.getSeconds(),
                scopeGrant.scope(),
                ACCESS_TOKEN_TYPE);
    }

    private Mono<Void> validateOrganizationAccess(UserAccount userAccount, UUID organizationId) {
        if (organizationId == null) {
            return Mono.empty();
        }
        return userOrganizationAccessDirectory.listUserOrganizations(userAccount.tenantId(), userAccount.id())
                .filter(access -> organizationId.equals(access.organizationId()))
                .next()
                .switchIfEmpty(Mono.error(OAuthException.invalidGrant(
                        "The selected organization is not accessible in this SSO context.")))
                .then();
    }

    private Mono<ClientApplication> authenticateClient(ClientAuthentication clientAuthentication) {
        String normalizedClientId = requireText(clientAuthentication.clientId(), "client_id");
        String normalizedClientSecret = requireText(clientAuthentication.clientSecret(), "client_secret");
        return authenticateClientApplicationUseCase.authenticate(normalizedClientId, normalizedClientSecret)
                .switchIfEmpty(Mono.error(OAuthException.invalidClient("Invalid client application credentials.")));
    }

    private Mono<OidcUserInfoPayload> toAccessTokenUserInfo(JWTClaimsSet claimsSet) {
        UUID tenantId = requireUuid(readStringClaim(claimsSet, "tid"), "tid");
        UUID userId = requireUuid(claimsSet.getSubject(), "sub");
        UUID organizationId = parseUuidQuietly(readStringClaim(claimsSet, "oid"));
        UUID agencyId = parseUuidQuietly(readStringClaim(claimsSet, "aid"));
        UUID actorId = parseUuidQuietly(readStringClaim(claimsSet, "actor"));
        List<String> permissions = readStringListClaim(claimsSet, "permissions");
        String clientId = readStringClaim(claimsSet, "azp");
        String serviceCode = readStringClaim(claimsSet, "svc");
        boolean sso = Boolean.TRUE.equals(readBooleanClaim(claimsSet, "sso"));

        return userAccountRepository.findById(tenantId, userId)
                .map(userAccount -> new OidcUserInfoPayload(
                        userId.toString(),
                        null,
                        userAccount.username(),
                        userAccount.email(),
                        tenantId,
                        organizationId,
                        agencyId,
                        actorId,
                        permissions,
                        clientId,
                        serviceCode,
                        sso,
                        List.of()))
                .switchIfEmpty(Mono.just(new OidcUserInfoPayload(
                        userId.toString(),
                        null,
                        null,
                        null,
                        tenantId,
                        organizationId,
                        agencyId,
                        actorId,
                        permissions,
                        clientId,
                        serviceCode,
                        sso,
                        List.of())));
    }

    private Mono<Map<String, Object>> introspectToken(
            String token,
            String tokenTypeHint,
            ClientApplication clientApplication) {
        Mono<Map<String, Object>> preferred = prefersSsoIntrospection(tokenTypeHint)
                ? introspectSsoSessionToken(token, clientApplication.clientId())
                        .switchIfEmpty(introspectAccessToken(token, clientApplication.clientId()))
                : introspectAccessToken(token, clientApplication.clientId())
                        .switchIfEmpty(introspectSsoSessionToken(token, clientApplication.clientId()));
        return preferred.defaultIfEmpty(inactiveIntrospection());
    }

    private Mono<Map<String, Object>> introspectAccessToken(String token, String introspectingClientId) {
        return Mono.justOrEmpty(jwtTokenService.decodeSignedToken(token))
                .filter(claimsSet -> !SSO_SESSION_TOKEN_KIND.equals(readStringClaim(claimsSet, "typ")))
                .map(claimsSet -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("active", true);
                    payload.put("client_id", readStringClaim(claimsSet, "azp"));
                    payload.put("token_type", "Bearer");
                    payload.put("sub", claimsSet.getSubject());
                    payload.put("scope", resolveScopeFromClaims(claimsSet));
                    payload.put("exp", toEpochSecond(claimsSet.getExpirationTime()));
                    payload.put("iat", toEpochSecond(claimsSet.getIssueTime()));
                    payload.put("aud", claimsSet.getAudience());
                    payload.put("iss", claimsSet.getIssuer());
                    payload.put("jti", claimsSet.getJWTID());
                    payload.put("tid", readStringClaim(claimsSet, "tid"));
                    payload.put("oid", readStringClaim(claimsSet, "oid"));
                    payload.put("aid", readStringClaim(claimsSet, "aid"));
                    payload.put("actor", readStringClaim(claimsSet, "actor"));
                    payload.put("permissions", readStringListClaim(claimsSet, "permissions"));
                    payload.put("svc", readStringClaim(claimsSet, "svc"));
                    payload.put("sso", readBooleanClaim(claimsSet, "sso"));
                    payload.put("introspected_by", introspectingClientId);
                    return payload;
                });
    }

    private Mono<Map<String, Object>> introspectSsoSessionToken(String token, String introspectingClientId) {
        return Mono.justOrEmpty(jwtTokenService.decodeSignedToken(token))
                .filter(claimsSet -> SSO_SESSION_TOKEN_KIND.equals(readStringClaim(claimsSet, "typ")))
                .map(claimsSet -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("active", true);
                    payload.put("client_id", introspectingClientId);
                    payload.put("token_type", "Bearer");
                    payload.put("sub", claimsSet.getSubject());
                    payload.put("sid", claimsSet.getJWTID());
                    payload.put("exp", toEpochSecond(claimsSet.getExpirationTime()));
                    payload.put("iat", toEpochSecond(claimsSet.getIssueTime()));
                    payload.put("aud", claimsSet.getAudience());
                    payload.put("iss", claimsSet.getIssuer());
                    payload.put("typ", readStringClaim(claimsSet, "typ"));
                    payload.put("principal", readStringClaim(claimsSet, "principal"));
                    payload.put("contexts", readContextIds(claimsSet));
                    payload.put("context_count", readContextIds(claimsSet).size());
                    return payload;
                });
    }

    private Map<String, Object> inactiveIntrospection() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("active", false);
        return payload;
    }

    private ScopeGrant resolveScopeGrant(
            PlatformServiceCode requestedService,
            Set<String> authorities,
            String requestedScope) {
        LinkedHashSet<String> grantedScopes = new LinkedHashSet<>();
        List<String> sortedAuthorities = authorities == null
                ? List.of()
                : authorities.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).sorted().toList();
        List<String> businessPermissions = sortedAuthorities.stream()
                .filter(authority -> !authority.startsWith("ROLE_"))
                .toList();
        if (businessPermissions.isEmpty()) {
            grantedScopes.add(requestedService.code());
        } else {
            grantedScopes.addAll(businessPermissions);
        }

        String normalizedRequestedScope = normalizeNullableText(requestedScope);
        if (normalizedRequestedScope == null) {
            return new ScopeGrant(String.join(" ", grantedScopes), Set.copyOf(businessPermissions));
        }

        LinkedHashSet<String> requestedScopes = splitScopes(normalizedRequestedScope);
        for (String scope : requestedScopes) {
            if (!grantedScopes.contains(scope)) {
                throw OAuthException.invalidScope("Requested scope '" + scope + "' is not granted.");
            }
        }

        LinkedHashSet<String> filteredPermissions = new LinkedHashSet<>();
        for (String authority : businessPermissions) {
            if (requestedScopes.contains(authority)) {
                filteredPermissions.add(authority);
            }
        }
        return new ScopeGrant(String.join(" ", requestedScopes), Set.copyOf(filteredPermissions));
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw OAuthException.invalidRequest(fieldName + " is required");
        }
        return value.trim();
    }

    private void requireEquals(String actual, String expected, OAuthException exception) {
        if (!Objects.equals(actual, expected)) {
            throw exception;
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private LinkedHashSet<String> splitScopes(String scope) {
        return java.util.Arrays.stream(scope.trim().split("\\s+"))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean prefersSsoIntrospection(String tokenTypeHint) {
        if (tokenTypeHint == null) {
            return false;
        }
        return SSO_SESSION_TOKEN_KIND.equals(tokenTypeHint)
                || "auth_sso_session".equalsIgnoreCase(tokenTypeHint);
    }

    private String resolveScopeFromClaims(JWTClaimsSet claimsSet) {
        List<String> permissions = readStringListClaim(claimsSet, "permissions");
        if (!permissions.isEmpty()) {
            return String.join(" ", permissions);
        }
        String serviceCode = readStringClaim(claimsSet, "svc");
        return serviceCode == null || serviceCode.isBlank() ? null : serviceCode;
    }

    private List<String> readContextIds(JWTClaimsSet claimsSet) {
        Object rawContexts = claimsSet.getClaim("contexts");
        if (!(rawContexts instanceof List<?> contexts)) {
            return List.of();
        }
        return contexts.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(context -> context.get("contextId"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private String readStringClaim(JWTClaimsSet claimsSet, String claimName) {
        try {
            return claimsSet.getStringClaim(claimName);
        } catch (java.text.ParseException exception) {
            return null;
        }
    }

    private Boolean readBooleanClaim(JWTClaimsSet claimsSet, String claimName) {
        Object value = claimsSet.getClaim(claimName);
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringListClaim(JWTClaimsSet claimsSet, String claimName) {
        Object value = claimsSet.getClaim(claimName);
        if (!(value instanceof List<?> rawValues)) {
            return List.of();
        }
        return ((List<Object>) rawValues).stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(text -> !text.isBlank())
                .sorted()
                .toList();
    }

    private Long toEpochSecond(java.util.Date value) {
        return value == null ? null : value.toInstant().getEpochSecond();
    }

    private UUID requireUuid(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw OAuthException.invalidToken("Required JWT claim '" + claimName + "' is missing.");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw OAuthException.invalidToken("JWT claim '" + claimName + "' is not a valid UUID.");
        }
    }

    private UUID parseUuidQuietly(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public record IssuedAccessToken(
            String accessToken,
            String tokenType,
            long expiresInSeconds,
            String scope,
            String issuedTokenType) {
    }

    public record ClientAuthentication(
            String clientId,
            String clientSecret) {
    }

    public record TokenExchangeRequest(
            String grantType,
            String subjectTokenType,
            String subjectToken,
            String contextId,
            UUID organizationId,
            UUID agencyId,
            String serviceCode,
            String requestedTokenType,
            String scope) {
    }

    public record ScopeGrant(
            String scope,
            Set<String> permissions) {
    }

    public record OidcUserInfoPayload(
            String sub,
            String sid,
            String preferredUsername,
            String email,
            UUID tenantId,
            UUID organizationId,
            UUID agencyId,
            UUID actorId,
            List<String> permissions,
            String clientId,
            String serviceCode,
            Boolean sso,
            List<AuthSharedSessionService.SharedSsoUserContext> contexts) {

        public static OidcUserInfoPayload fromSharedSession(AuthSharedSessionService.SharedSsoUserInfo userInfo) {
            return new OidcUserInfoPayload(
                    userInfo.subject(),
                    userInfo.sessionId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    Boolean.TRUE,
                    userInfo.contexts());
        }
    }

    public static final class OAuthException extends RuntimeException {

        private final String error;
        private final int status;

        private OAuthException(String error, String description, int status) {
            super(description);
            this.error = error;
            this.status = status;
        }

        public static OAuthException invalidRequest(String description) {
            return new OAuthException("invalid_request", description, 400);
        }

        public static OAuthException invalidClient(String description) {
            return new OAuthException("invalid_client", description, 401);
        }

        public static OAuthException invalidGrant(String description) {
            return new OAuthException("invalid_grant", description, 400);
        }

        public static OAuthException invalidScope(String description) {
            return new OAuthException("invalid_scope", description, 400);
        }

        public static OAuthException invalidToken(String description) {
            return new OAuthException("invalid_token", description, 401);
        }

        public String error() {
            return error;
        }

        public int status() {
            return status;
        }
    }
}
