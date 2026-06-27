package yowyob.comops.api.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import yowyob.comops.api.auth.adapter.out.persistence.InMemoryUserAccountRepository;
import yowyob.comops.api.auth.application.service.AuthOidcService.ClientAuthentication;
import yowyob.comops.api.auth.application.service.AuthOidcService.IssuedAccessToken;
import yowyob.comops.api.auth.application.service.AuthOidcService.OAuthException;
import yowyob.comops.api.auth.application.service.AuthOidcService.TokenExchangeRequest;
import yowyob.comops.api.auth.config.AuthSsoProperties;
import yowyob.comops.api.auth.domain.model.UserAccount;
import yowyob.comops.api.kernel.config.JwtTokenService;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;
import yowyob.comops.api.kernel.domain.model.ClientApplication;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests unitaires pour AuthOidcService — l'étape finale du flux YowAuth0.
 *
 * Ce service est le "guichet d'échange" OAuth2 :
 *   Token SSO (passeport universel)  →  Access Token JWT (badge pour un service précis)
 *
 * Analogie : après que tu as montré ton passeport à l'accueil (SSO login),
 * tu échanges ce passeport contre un badge d'accès spécifique à un bâtiment (SALES, RESOURCE...).
 * Le badge contient tes droits (permissions) pour CE bâtiment seulement.
 *
 * Flux complet :
 *   1. Application cliente s'authentifie (clientId + clientSecret)
 *   2. Vérifie le SSO token et extrait le contexte demandé (tenantId, userId)
 *   3. Résout les permissions de l'utilisateur
 *   4. Émet un JWT Access Token signé RS256 contenant : tenantId, orgId, permissions, serviceCode
 *
 * `openIdConfiguration()` est une fonction pure (pas de dépendances au moment de l'appel)
 * qui expose le document de découverte OIDC standard.
 */
class AuthOidcServiceTest {

    // Infrastructure partagée
    private JwtTokenService jwtTokenService;
    private AuthSsoSessionTokenService ssoTokenService;
    private InMemoryUserAccountRepository userAccountRepository;
    private AuthOidcService oidcService;

    // Application cliente fictive autorisée pour le service SALES
    private static final String CLIENT_ID     = "test-client";
    private static final String CLIENT_SECRET = "not-checked-in-unit-test";

    @BeforeEach
    void setUp() {
        // JWT avec clé générée en mémoire — même config que les autres tests
        SecurityRuntimeProperties props = new SecurityRuntimeProperties();
        props.getJwt().setAutoGenerateKeyPair(true);
        props.getJwt().setIssuer("yowyob-test");
        jwtTokenService = new JwtTokenService(props);

        AuthSsoProperties ssoProps = new AuthSsoProperties();
        ssoProps.setSessionTtl(Duration.ofHours(8));
        ssoTokenService = new AuthSsoSessionTokenService(jwtTokenService, ssoProps);

        userAccountRepository = new InMemoryUserAccountRepository();

        // ClientApplication autorisée sur le service SALES
        ClientApplication clientApp = ClientApplication.register(
                CLIENT_ID, "Test Client", "Test application",
                "hashed-secret",   // le secret n'est pas vérifié ici (on mocke l'authentification)
                Set.of("SALES"),
                false);

        // AuthSharedSessionService : nécessaire pour userInfo() sur token SSO
        AuthSharedSessionService sharedSessionService = new AuthSharedSessionService(
                userAccountRepository,
                (tid, uid) -> Flux.empty(),
                ssoTokenService);

        oidcService = new AuthOidcService(
                // AuthenticateClientApplicationUseCase : retourne toujours clientApp (mock)
                (clientId, clientSecret) -> CLIENT_ID.equals(clientId)
                        ? Mono.just(clientApp)
                        : Mono.empty(),
                userAccountRepository,
                // UserOrganizationAccessDirectory : pas d'organisations dans ces tests
                (tid, uid) -> Flux.empty(),
                // ReactivePermissionResolver : permissions vides (focus sur le flux, pas les droits)
                (tid, uid) -> Mono.just(Set.of()),
                jwtTokenService,
                ssoTokenService,
                sharedSessionService,
                // RecordSystemAuditUseCase : on ignore les événements d'audit
                (tid, orgId, uid, action, type, ref, payload) -> Mono.empty()
        );
    }

    /** Crée un compte et le sauvegarde en mémoire. */
    private UserAccount saveAccount(UUID tenantId) {
        UserAccount account = UserAccount.register(
                tenantId, UUID.randomUUID(), "jean", "jean@example.com",
                new BCryptPasswordEncoder().encode("Passw0rd!secure"), "LOCAL");
        return userAccountRepository.save(account).block();
    }

    /** Émet un vrai SSO session token pour un utilisateur donné. */
    private String issueSsoToken(UUID tenantId, UUID userId, UUID actorId) {
        String contextId = UUID.randomUUID().toString();
        return ssoTokenService.issue(
                "jean@example.com",
                java.util.List.of(new AuthSsoSessionTokenService.SsoSessionContext(
                        contextId, tenantId, userId, actorId))
        ).token();
    }

    // =========================================================================
    // exchangeToken() — échange du token SSO contre un access token
    // =========================================================================

    @Nested
    @DisplayName("exchangeToken() — échange OAuth2")
    class ExchangeToken {

        @Test
        @DisplayName("échange réussi : retourne un access token JWT valide")
        void shouldIssueAccessTokenForValidSsoToken() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = saveAccount(tenantId);
            String ssoToken  = issueSsoToken(tenantId, account.id(), account.actorId());

            // On récupère le contextId depuis le token SSO vérifié
            String contextId = ssoTokenService.verify(ssoToken)
                    .orElseThrow().contexts().get(0).contextId();

            TokenExchangeRequest request = new TokenExchangeRequest(
                    AuthOidcService.TOKEN_EXCHANGE_GRANT,
                    AuthOidcService.JWT_SUBJECT_TOKEN_TYPE,
                    ssoToken,
                    contextId,
                    null,   // pas d'organisation spécifique
                    null,   // pas d'agence spécifique
                    "SALES",
                    null,
                    null);

            IssuedAccessToken issued = oidcService.exchangeToken(
                    new ClientAuthentication(CLIENT_ID, CLIENT_SECRET),
                    request).block();

            assertThat(issued).isNotNull();
            assertThat(issued.accessToken()).isNotBlank();
            assertThat(issued.tokenType()).isEqualTo("Bearer");
            assertThat(issued.expiresInSeconds()).isGreaterThan(0);
            assertThat(issued.issuedTokenType()).isEqualTo(AuthOidcService.ACCESS_TOKEN_TYPE);
        }

        @Test
        @DisplayName("le token d'accès contient le tenantId et le serviceCode dans ses claims")
        void accessTokenShouldContainTenantAndServiceClaims() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = saveAccount(tenantId);
            String ssoToken = issueSsoToken(tenantId, account.id(), account.actorId());
            String contextId = ssoTokenService.verify(ssoToken)
                    .orElseThrow().contexts().get(0).contextId();

            IssuedAccessToken issued = oidcService.exchangeToken(
                    new ClientAuthentication(CLIENT_ID, CLIENT_SECRET),
                    new TokenExchangeRequest(
                            AuthOidcService.TOKEN_EXCHANGE_GRANT,
                            AuthOidcService.JWT_SUBJECT_TOKEN_TYPE,
                            ssoToken, contextId, null, null, "SALES", null, null))
                    .block();

            // Décoder le token pour vérifier son contenu
            var claims = jwtTokenService.decodeSignedToken(issued.accessToken()).orElseThrow();

            assertThat(claims.getSubject())
                    .as("Le subject doit être l'userId")
                    .isEqualTo(account.id().toString());

            String extractedTid = getStringClaim(claims, "tid");
            assertThat(extractedTid)
                    .as("Le tenantId doit être dans le token")
                    .isEqualTo(tenantId.toString());

            String extractedSvc = getStringClaim(claims, "svc");
            assertThat(extractedSvc)
                    .as("Le serviceCode doit être SALES")
                    .isEqualTo("SALES");
        }

        @Test
        @DisplayName("échoue avec OAuthException si le SSO token est invalide")
        void shouldFailForInvalidSsoToken() {
            StepVerifier.create(oidcService.exchangeToken(
                    new ClientAuthentication(CLIENT_ID, CLIENT_SECRET),
                    new TokenExchangeRequest(
                            AuthOidcService.TOKEN_EXCHANGE_GRANT,
                            AuthOidcService.JWT_SUBJECT_TOKEN_TYPE,
                            "token.sso.invalide",
                            "context-id",
                            null, null, "SALES", null, null)))
                    .expectErrorMatches(e -> e instanceof OAuthException
                            && "invalid_grant".equals(((OAuthException) e).error()))
                    .verify();
        }

        @Test
        @DisplayName("échoue avec OAuthException si le contextId est inconnu dans le token SSO")
        void shouldFailForUnknownContextId() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = saveAccount(tenantId);
            String ssoToken = issueSsoToken(tenantId, account.id(), account.actorId());

            StepVerifier.create(oidcService.exchangeToken(
                    new ClientAuthentication(CLIENT_ID, CLIENT_SECRET),
                    new TokenExchangeRequest(
                            AuthOidcService.TOKEN_EXCHANGE_GRANT,
                            AuthOidcService.JWT_SUBJECT_TOKEN_TYPE,
                            ssoToken,
                            "context-qui-nexiste-pas",  // contextId invalide
                            null, null, "SALES", null, null)))
                    .expectErrorMatches(e -> e instanceof OAuthException
                            && "invalid_grant".equals(((OAuthException) e).error()))
                    .verify();
        }

        @Test
        @DisplayName("échoue avec OAuthException si le client n'est pas autorisé pour ce service")
        void shouldFailWhenClientCannotAccessRequestedService() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = saveAccount(tenantId);
            String ssoToken = issueSsoToken(tenantId, account.id(), account.actorId());
            String contextId = ssoTokenService.verify(ssoToken)
                    .orElseThrow().contexts().get(0).contextId();

            // Le client est autorisé seulement pour SALES, pas pour RESOURCE
            StepVerifier.create(oidcService.exchangeToken(
                    new ClientAuthentication(CLIENT_ID, CLIENT_SECRET),
                    new TokenExchangeRequest(
                            AuthOidcService.TOKEN_EXCHANGE_GRANT,
                            AuthOidcService.JWT_SUBJECT_TOKEN_TYPE,
                            ssoToken, contextId,
                            null, null, "RESOURCE", null, null)))  // RESOURCE non autorisé
                    .expectErrorMatches(e -> e instanceof OAuthException
                            && "invalid_grant".equals(((OAuthException) e).error()))
                    .verify();
        }

        @Test
        @DisplayName("échoue avec OAuthException si le grant_type est incorrect")
        void shouldFailForWrongGrantType() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = saveAccount(tenantId);
            String ssoToken = issueSsoToken(tenantId, account.id(), account.actorId());

            // La validation grant_type est synchrone → exception lancée avant création du Mono
            assertThatThrownBy(() -> oidcService.exchangeToken(
                    new ClientAuthentication(CLIENT_ID, CLIENT_SECRET),
                    new TokenExchangeRequest(
                            "authorization_code",   // mauvais grant_type
                            AuthOidcService.JWT_SUBJECT_TOKEN_TYPE,
                            ssoToken, "ctx", null, null, "SALES", null, null)).block())
                    .isInstanceOf(OAuthException.class)
                    .satisfies(e -> assertThat(((OAuthException) e).error()).isEqualTo("invalid_request"));
        }

        @Test
        @DisplayName("échoue avec OAuthException si les credentials client sont invalides")
        void shouldFailForInvalidClientCredentials() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = saveAccount(tenantId);
            String ssoToken = issueSsoToken(tenantId, account.id(), account.actorId());
            String contextId = ssoTokenService.verify(ssoToken)
                    .orElseThrow().contexts().get(0).contextId();

            // "unknown-client" ne retourne rien dans notre mock
            StepVerifier.create(oidcService.exchangeToken(
                    new ClientAuthentication("unknown-client", "bad-secret"),
                    new TokenExchangeRequest(
                            AuthOidcService.TOKEN_EXCHANGE_GRANT,
                            AuthOidcService.JWT_SUBJECT_TOKEN_TYPE,
                            ssoToken, contextId, null, null, "SALES", null, null)))
                    .expectErrorMatches(e -> e instanceof OAuthException
                            && "invalid_client".equals(((OAuthException) e).error()))
                    .verify();
        }
    }

    // =========================================================================
    // openIdConfiguration() — document de découverte OIDC
    // =========================================================================

    @Nested
    @DisplayName("openIdConfiguration() — document de découverte OIDC")
    class OpenIdConfiguration {

        @Test
        @DisplayName("contient les champs obligatoires du standard OIDC")
        void shouldContainRequiredOidcFields() {
            Map<String, Object> config = oidcService.openIdConfiguration(
                    "https://auth.yowyob.com",
                    "https://auth.yowyob.com/oauth/token",
                    "https://auth.yowyob.com/oauth/userinfo",
                    "https://auth.yowyob.com/.well-known/jwks.json",
                    "https://auth.yowyob.com/oauth/introspect");

            // Champs obligatoires OIDC
            assertThat(config).containsKey("issuer");
            assertThat(config).containsKey("jwks_uri");
            assertThat(config).containsKey("token_endpoint");
            assertThat(config).containsKey("userinfo_endpoint");
        }

        @Test
        @DisplayName("l'issuer correspond à celui configuré dans JwtTokenService")
        void issuerShouldMatchJwtConfiguration() {
            Map<String, Object> config = oidcService.openIdConfiguration(
                    "https://auth.yowyob.com",
                    "https://auth.yowyob.com/oauth/token",
                    "https://auth.yowyob.com/oauth/userinfo",
                    "https://auth.yowyob.com/.well-known/jwks.json",
                    "https://auth.yowyob.com/oauth/introspect");

            assertThat(config.get("issuer"))
                    .as("L'issuer doit correspondre à celui configuré : 'yowyob-test'")
                    .isEqualTo("yowyob-test");
        }

        @Test
        @DisplayName("déclare RS256 comme algorithme de signature des tokens")
        void shouldDeclareRS256AsSigningAlgorithm() {
            Map<String, Object> config = oidcService.openIdConfiguration(
                    "https://auth.yowyob.com", "/token", "/userinfo", "/jwks", "/introspect");

            @SuppressWarnings("unchecked")
            java.util.List<String> algorithms =
                    (java.util.List<String>) config.get("id_token_signing_alg_values_supported");

            assertThat(algorithms).contains("RS256");
        }

        @Test
        @DisplayName("déclare le grant type token-exchange supporté")
        void shouldDeclareTokenExchangeGrantType() {
            Map<String, Object> config = oidcService.openIdConfiguration(
                    "https://auth.yowyob.com", "/token", "/userinfo", "/jwks", "/introspect");

            @SuppressWarnings("unchecked")
            java.util.List<String> grantTypes =
                    (java.util.List<String>) config.get("grant_types_supported");

            assertThat(grantTypes).contains(AuthOidcService.TOKEN_EXCHANGE_GRANT);
        }
    }

    // =========================================================================
    // Méthode utilitaire
    // =========================================================================

    private String getStringClaim(com.nimbusds.jwt.JWTClaimsSet claimsSet, String claim) {
        try {
            return claimsSet.getStringClaim(claim);
        } catch (java.text.ParseException e) {
            return null;
        }
    }
}
