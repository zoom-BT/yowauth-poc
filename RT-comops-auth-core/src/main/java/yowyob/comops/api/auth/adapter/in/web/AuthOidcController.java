package yowyob.comops.api.auth.adapter.in.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.application.service.AuthOidcService;

@RestController
public class AuthOidcController {

    private final AuthOidcService authOidcService;

    public AuthOidcController(AuthOidcService authOidcService) {
        this.authOidcService = authOidcService;
    }

    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> openIdConfiguration(ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            String base = baseUrl(exchange);
            return ResponseEntity.ok(authOidcService.openIdConfiguration(
                    base,
                    base + "/oauth2/token",
                    base + "/oauth2/userinfo",
                    base + "/.well-known/jwks.json",
                    base + "/oauth2/introspect"));
        });
    }

    @GetMapping(value = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> authorizationServerMetadata(ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            String base = baseUrl(exchange);
            return ResponseEntity.ok(authOidcService.openIdConfiguration(
                    base,
                    base + "/oauth2/token",
                    base + "/oauth2/userinfo",
                    base + "/.well-known/jwks.json",
                    base + "/oauth2/introspect"));
        });
    }

    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> token(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(form -> authOidcService.exchangeToken(
                                resolveClientAuthentication(form, authorizationHeader),
                                new AuthOidcService.TokenExchangeRequest(
                                        form.getFirst("grant_type"),
                                        form.getFirst("subject_token_type"),
                                        form.getFirst("subject_token"),
                                        form.getFirst("context_id"),
                                        parseUuid(form.getFirst("organization_id")),
                                        parseUuid(form.getFirst("agency_id")),
                                        form.getFirst("service_code"),
                                        form.getFirst("requested_token_type"),
                                        form.getFirst("scope")))
                        .map(issued -> ResponseEntity.ok().<Object>body(new OAuthTokenResponse(
                                issued.accessToken(),
                                issued.tokenType(),
                                issued.expiresInSeconds(),
                                issued.scope(),
                                issued.issuedTokenType()))))
                .onErrorResume(AuthOidcService.OAuthException.class,
                        exception -> Mono.just(oauthError(exception)));
    }

    @RequestMapping(value = "/oauth2/userinfo", method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> userInfo(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, bearerAuthenticateValue("Missing bearer token."))
                    .body(new OAuthErrorResponse("invalid_token", "Missing bearer token.")));
        }
        return authOidcService.userInfo(token)
                .map(userInfo -> ResponseEntity.ok().<Object>body(OidcUserInfoResponse.from(userInfo)))
                .onErrorResume(AuthOidcService.OAuthException.class,
                        exception -> Mono.just(oauthError(exception)));
    }

    @PostMapping(value = "/oauth2/introspect", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> introspect(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(form -> authOidcService.introspect(
                                resolveClientAuthentication(form, authorizationHeader),
                                form.getFirst("token"),
                                form.getFirst("token_type_hint"))
                        .map(body -> ResponseEntity.ok().<Object>body(body)))
                .onErrorResume(AuthOidcService.OAuthException.class,
                        exception -> Mono.just(oauthError(exception)));
    }

    private String baseUrl(ServerWebExchange exchange) {
        URI uri = exchange.getRequest().getURI();
        if (uri.getScheme() == null || uri.getHost() == null) {
            return "http://localhost";
        }
        int port = uri.getPort();
        boolean defaultPort = port < 0
                || ("http".equalsIgnoreCase(uri.getScheme()) && port == 80)
                || ("https".equalsIgnoreCase(uri.getScheme()) && port == 443);
        return uri.getScheme() + "://" + uri.getHost() + (defaultPort ? "" : ":" + port);
    }

    private UUID parseUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawValue.trim());
        } catch (IllegalArgumentException exception) {
            throw AuthOidcService.OAuthException.invalidRequest("Invalid UUID value: " + rawValue);
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (!authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }

    private AuthOidcService.ClientAuthentication resolveClientAuthentication(
            MultiValueMap<String, String> form,
            String authorizationHeader) {
        AuthOidcService.ClientAuthentication basicAuthentication = parseBasicClientAuthentication(authorizationHeader);
        if (basicAuthentication != null) {
            return basicAuthentication;
        }
        return new AuthOidcService.ClientAuthentication(form.getFirst("client_id"), form.getFirst("client_secret"));
    }

    private AuthOidcService.ClientAuthentication parseBasicClientAuthentication(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (!authorizationHeader.regionMatches(true, 0, "Basic ", 0, 6)) {
            return null;
        }
        try {
            String encodedValue = authorizationHeader.substring(6).trim();
            String decoded = new String(Base64.getDecoder().decode(encodedValue), StandardCharsets.UTF_8);
            int separatorIndex = decoded.indexOf(':');
            if (separatorIndex < 0) {
                throw AuthOidcService.OAuthException.invalidClient("Malformed client basic authentication header.");
            }
            return new AuthOidcService.ClientAuthentication(
                    decoded.substring(0, separatorIndex),
                    decoded.substring(separatorIndex + 1));
        } catch (IllegalArgumentException exception) {
            throw AuthOidcService.OAuthException.invalidClient("Malformed client basic authentication header.");
        }
    }

    private ResponseEntity<Object> oauthError(AuthOidcService.OAuthException exception) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.status());
        if ("invalid_client".equals(exception.error())) {
            response.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"iwm-oidc\"");
        }
        if ("invalid_token".equals(exception.error())) {
            response.header(HttpHeaders.WWW_AUTHENTICATE, bearerAuthenticateValue(exception.getMessage()));
        }
        return response.body(new OAuthErrorResponse(exception.error(), exception.getMessage()));
    }

    private String bearerAuthenticateValue(String description) {
        return "Bearer error=\"invalid_token\", error_description=\"" + description.replace("\"", "'") + "\"";
    }
}
