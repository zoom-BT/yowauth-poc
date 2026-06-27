package yowyob.comops.api.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import yowyob.comops.api.auth.application.service.AuthSsoSessionTokenService.IssuedSsoSessionToken;
import yowyob.comops.api.auth.application.service.AuthSsoSessionTokenService.SsoSessionContext;
import yowyob.comops.api.auth.application.service.AuthSsoSessionTokenService.VerifiedSsoSessionToken;
import yowyob.comops.api.auth.config.AuthSsoProperties;
import yowyob.comops.api.kernel.config.JwtTokenService;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires pour AuthSsoSessionTokenService — le "passeport SSO" de YowAuth0.
 *
 * Analogie : c'est le passeport qui est délivré après un contrôle d'identité réussi.
 * - Il contient les informations sur qui on est (subject)
 * - Et dans quels "pays" (tenants/contexts) on a accès
 * - Il est signé de façon infalsifiable (RS256)
 * - Il expire après un certain temps (défaut : 8 heures)
 *
 * Ce token SSO sert ensuite pour l'échange de token OAuth2 :
 *   SSO session token → (token exchange) → Access Token JWT pour un service spécifique
 *
 * IMPORTANT : Le token SSO est du type "auth-sso-session".
 * Si on essaie de le vérifier avec le mauvais type de service, il est rejeté.
 */
class AuthSsoSessionTokenServiceTest {

    private AuthSsoSessionTokenService ssoTokenService;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        SecurityRuntimeProperties props = new SecurityRuntimeProperties();
        props.getJwt().setAutoGenerateKeyPair(true);
        props.getJwt().setIssuer("yowyob-test");
        jwtTokenService = new JwtTokenService(props);

        AuthSsoProperties ssoProps = new AuthSsoProperties();
        ssoProps.setSessionTtl(Duration.ofHours(8));
        ssoTokenService = new AuthSsoSessionTokenService(jwtTokenService, ssoProps);
    }

    // Données réutilisables
    private SsoSessionContext makeContext(UUID tenantId, UUID userId) {
        return new SsoSessionContext(UUID.randomUUID().toString(), tenantId, userId, UUID.randomUUID());
    }

    // =========================================================================
    // Émission du token SSO
    // =========================================================================

    @Nested
    @DisplayName("issue() — émission du token SSO")
    class Issue {

        @Test
        @DisplayName("émet un token non vide avec une durée de vie positive")
        void shouldIssueNonBlankTokenWithPositiveTtl() {
            UUID tenantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            IssuedSsoSessionToken issued = ssoTokenService.issue(
                    "jean@example.com",
                    List.of(makeContext(tenantId, userId)));

            assertThat(issued.token()).isNotBlank();
            assertThat(issued.expiresInSeconds())
                    .as("La durée de vie doit être positive")
                    .isGreaterThan(0);
        }

        @Test
        @DisplayName("refuse un subject null")
        void shouldRejectNullSubject() {
            assertThatThrownBy(() ->
                    ssoTokenService.issue(null, List.of(makeContext(UUID.randomUUID(), UUID.randomUUID()))))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("refuse une liste de contextes vide")
        void shouldRejectEmptyContextList() {
            assertThatThrownBy(() ->
                    ssoTokenService.issue("jean@example.com", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // Vérification du token SSO
    // =========================================================================

    @Nested
    @DisplayName("verify() — vérification et décodage du token SSO")
    class Verify {

        @Test
        @DisplayName("un token valide est vérifié et ses contextes sont extraits")
        void shouldVerifyValidTokenAndExtractContexts() {
            UUID tenantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID actorId = UUID.randomUUID();
            String contextId = UUID.randomUUID().toString();
            String subject = "jean@example.com";

            SsoSessionContext context = new SsoSessionContext(contextId, tenantId, userId, actorId);
            IssuedSsoSessionToken issued = ssoTokenService.issue(subject, List.of(context));

            Optional<VerifiedSsoSessionToken> verified = ssoTokenService.verify(issued.token());

            assertThat(verified).isPresent();
            assertThat(verified.get().subject()).isEqualTo(subject);
            assertThat(verified.get().contexts()).hasSize(1);

            var extractedContext = verified.get().contexts().get(0);
            assertThat(extractedContext.tenantId()).isEqualTo(tenantId);
            assertThat(extractedContext.userId()).isEqualTo(userId);
            assertThat(extractedContext.actorId()).isEqualTo(actorId);
        }

        @Test
        @DisplayName("tous les contextes multi-tenant sont préservés dans le token")
        void shouldPreserveMultipleContexts() {
            UUID tenantA = UUID.randomUUID();
            UUID tenantB = UUID.randomUUID();
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();

            List<SsoSessionContext> contexts = List.of(
                    makeContext(tenantA, userA),
                    makeContext(tenantB, userB));

            IssuedSsoSessionToken issued = ssoTokenService.issue("jean@example.com", contexts);
            VerifiedSsoSessionToken verified = ssoTokenService.verify(issued.token()).orElseThrow();

            assertThat(verified.contexts())
                    .as("Les deux contextes doivent être présents dans le token")
                    .hasSize(2);

            // verified.contexts() est List<VerifiedSsoSessionContext> — record direct
            List<UUID> tenantIds = verified.contexts().stream()
                    .map(AuthSsoSessionTokenService.VerifiedSsoSessionContext::tenantId)
                    .toList();

            assertThat(tenantIds).contains(tenantA, tenantB);
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token avec signature corrompue")
        void shouldRejectTamperedToken() {
            IssuedSsoSessionToken issued = ssoTokenService.issue(
                    "jean@example.com",
                    List.of(makeContext(UUID.randomUUID(), UUID.randomUUID())));

            String tampered = issued.token().substring(0, issued.token().length() - 10) + "CORROMPU!!";

            assertThat(ssoTokenService.verify(tampered)).isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token null ou vide")
        void shouldRejectNullOrBlankToken() {
            assertThat(ssoTokenService.verify(null)).isEmpty();
            assertThat(ssoTokenService.verify("")).isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token de type 'context-selection' (mauvais type)")
        void shouldRejectContextSelectionToken() {
            // Le service de sélection de contexte utilise un autre type de token
            // → le service SSO ne doit pas l'accepter
            AuthContextSelectionTokenService contextService =
                    new AuthContextSelectionTokenService(jwtTokenService);

            yowyob.comops.api.auth.application.port.in.SelectableLoginContext fakeContext =
                    new yowyob.comops.api.auth.application.port.in.SelectableLoginContext(
                            UUID.randomUUID().toString(),
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            List.of());

            String contextSelectionToken = contextService.issue("jean@example.com", List.of(fakeContext)).token();

            // Le service SSO ne doit PAS accepter un token de type context-selection
            assertThat(ssoTokenService.verify(contextSelectionToken))
                    .as("Un token de type 'context-selection' ne doit pas être accepté comme token SSO")
                    .isEmpty();
        }

        @Test
        @DisplayName("le sessionId est non nul (identifiant unique de la session)")
        void shouldHaveNonNullSessionId() {
            IssuedSsoSessionToken issued = ssoTokenService.issue(
                    "jean@example.com",
                    List.of(makeContext(UUID.randomUUID(), UUID.randomUUID())));

            VerifiedSsoSessionToken verified = ssoTokenService.verify(issued.token()).orElseThrow();

            assertThat(verified.sessionId())
                    .as("Chaque token SSO doit avoir un sessionId unique (JWT ID)")
                    .isNotBlank();
        }
    }

    // =========================================================================
    // Durée de vie configurable
    // =========================================================================

    @Nested
    @DisplayName("TTL — durée de vie configurable")
    class Ttl {

        @Test
        @DisplayName("la durée de vie reflète la configuration AuthSsoProperties")
        void shouldRespectConfiguredTtl() {
            AuthSsoProperties shortTtl = new AuthSsoProperties();
            shortTtl.setSessionTtl(Duration.ofMinutes(30));
            AuthSsoSessionTokenService shortService = new AuthSsoSessionTokenService(jwtTokenService, shortTtl);

            IssuedSsoSessionToken issued = shortService.issue(
                    "jean@example.com",
                    List.of(makeContext(UUID.randomUUID(), UUID.randomUUID())));

            assertThat(issued.expiresInSeconds())
                    .as("Le TTL doit correspondre à 30 minutes = 1800 secondes")
                    .isEqualTo(Duration.ofMinutes(30).getSeconds());
        }
    }
}
