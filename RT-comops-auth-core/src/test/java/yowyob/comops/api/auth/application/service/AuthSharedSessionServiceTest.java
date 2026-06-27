package yowyob.comops.api.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import yowyob.comops.api.auth.adapter.out.persistence.InMemoryUserAccountRepository;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccess;
import yowyob.comops.api.auth.application.service.AuthSharedSessionService.SharedSsoUserInfo;
import yowyob.comops.api.auth.config.AuthSsoProperties;
import yowyob.comops.api.auth.domain.model.UserAccount;
import yowyob.comops.api.kernel.config.JwtTokenService;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Tests unitaires pour AuthSharedSessionService.
 *
 * Ce service est l'étape finale de la connexion YowAuth0 :
 * après que l'utilisateur a choisi son contexte, on émet un token SSO partagé
 * qui représente la "session active" de l'utilisateur.
 *
 * Flux :
 *   1. issueForUser(userAccount)
 *      → cherche tous les comptes avec le même email (multi-tenant)
 *      → crée un contexte SSO pour chacun
 *      → émet un token SSO signé JWT
 *
 *   2. userInfo(ssoToken)
 *      → décode le token SSO
 *      → pour chaque contexte : récupère les organisations accessibles
 *      → retourne un résumé complet de la session
 */
class AuthSharedSessionServiceTest {

    private InMemoryUserAccountRepository userAccountRepository;
    private AuthSsoSessionTokenService ssoTokenService;
    private AuthSharedSessionService sharedSessionService;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        userAccountRepository = new InMemoryUserAccountRepository();

        SecurityRuntimeProperties props = new SecurityRuntimeProperties();
        props.getJwt().setAutoGenerateKeyPair(true);
        props.getJwt().setIssuer("yowyob-test");
        jwtTokenService = new JwtTokenService(props);

        AuthSsoProperties ssoProps = new AuthSsoProperties();
        ssoProps.setSessionTtl(Duration.ofHours(8));
        ssoTokenService = new AuthSsoSessionTokenService(jwtTokenService, ssoProps);

        sharedSessionService = new AuthSharedSessionService(
                userAccountRepository,
                // UserOrganizationAccessDirectory : pas d'organisations dans ces tests
                (tid, uid) -> Flux.empty(),
                ssoTokenService);
    }

    /** Crée et sauvegarde un compte en mémoire. */
    private UserAccount saveAccount(UUID tenantId, String username, String email) {
        UserAccount account = UserAccount.register(
                tenantId, UUID.randomUUID(), username, email,
                new BCryptPasswordEncoder().encode("Passw0rd!secure"), "LOCAL");
        return userAccountRepository.save(account).block();
    }

    // =========================================================================
    // issueForUser() — émission du token SSO
    // =========================================================================

    @Nested
    @DisplayName("issueForUser() — émission du token SSO partagé")
    class IssueForUser {

        @Test
        @DisplayName("émet un token SSO valide pour un utilisateur avec un seul compte")
        void shouldIssueSsoTokenForSingleTenantUser() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = saveAccount(tenantId, "jean", "jean@example.com");

            AuthSsoSessionTokenService.IssuedSsoSessionToken issued =
                    sharedSessionService.issueForUser(account).block();

            assertThat(issued).isNotNull();
            assertThat(issued.token()).isNotBlank();
            assertThat(issued.expiresInSeconds()).isGreaterThan(0);

            // Le token doit être vérifiable
            assertThat(ssoTokenService.verify(issued.token())).isPresent();
        }

        @Test
        @DisplayName("le token SSO contient un contexte par tenant quand l'email est multi-tenant")
        void shouldEmbedOneContextPerTenantForMultiTenantUser() {
            // Même email dans deux tenants différents
            UUID tenantA = UUID.randomUUID();
            UUID tenantB = UUID.randomUUID();
            UserAccount accountA = saveAccount(tenantA, "jean", "jean@example.com");
            saveAccount(tenantB, "jean", "jean@example.com");

            // On émet le token depuis le compte du tenant A
            // → mais le service cherche tous les comptes avec cet email (multi-tenant)
            AuthSsoSessionTokenService.IssuedSsoSessionToken issued =
                    sharedSessionService.issueForUser(accountA).block();

            var verified = ssoTokenService.verify(issued.token()).orElseThrow();

            assertThat(verified.contexts())
                    .as("Les deux contextes (tenant A et B) doivent être dans le token")
                    .hasSize(2);

            // VerifiedSsoSessionContext est un record direct de AuthSsoSessionTokenService
            assertThat(verified.contexts().stream()
                    .map(AuthSsoSessionTokenService.VerifiedSsoSessionContext::tenantId)
                    .toList())
                    .contains(tenantA, tenantB);
        }

        @Test
        @DisplayName("le subject du token SSO est l'email de l'utilisateur")
        void shouldUseEmailAsSubject() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = saveAccount(tenantId, "jean", "jean@example.com");

            AuthSsoSessionTokenService.IssuedSsoSessionToken issued =
                    sharedSessionService.issueForUser(account).block();

            var verified = ssoTokenService.verify(issued.token()).orElseThrow();

            assertThat(verified.subject())
                    .as("Le subject du token SSO doit être l'email")
                    .isEqualTo("jean@example.com");
        }
    }

    // =========================================================================
    // userInfo() — lecture des informations de session
    // =========================================================================

    @Nested
    @DisplayName("userInfo() — informations de la session SSO")
    class UserInfo {

        @Test
        @DisplayName("retourne les informations correctes pour un token SSO valide")
        void shouldReturnUserInfoForValidSsoToken() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = saveAccount(tenantId, "jean", "jean@example.com");

            String ssoToken = sharedSessionService.issueForUser(account).block().token();

            SharedSsoUserInfo info = sharedSessionService.userInfo(ssoToken).block();

            assertThat(info).isNotNull();
            assertThat(info.subject()).isEqualTo("jean@example.com");
            assertThat(info.sessionId()).isNotBlank();
            assertThat(info.contexts()).hasSize(1);
            assertThat(info.contexts().get(0).tenantId()).isEqualTo(tenantId);
            assertThat(info.contexts().get(0).userId()).isEqualTo(account.id());
        }

        @Test
        @DisplayName("les organisations accessibles sont incluses dans chaque contexte")
        void shouldIncludeOrganizationsInContexts() {
            UUID tenantId = UUID.randomUUID();
            UUID orgId = UUID.randomUUID();
            UserAccount account = saveAccount(tenantId, "jean", "jean@example.com");

            // On recrée le service avec une organisation accessible
            AuthSharedSessionService serviceWithOrg = new AuthSharedSessionService(
                    userAccountRepository,
                    (tid, uid) -> Flux.just(
                            new UserOrganizationAccess(orgId, "ORG-001", "Mon Org", "Mon Organisation",
                                    List.of("SALES"))),
                    ssoTokenService);

            String ssoToken = serviceWithOrg.issueForUser(account).block().token();
            SharedSsoUserInfo info = serviceWithOrg.userInfo(ssoToken).block();

            assertThat(info.contexts().get(0).organizations())
                    .as("L'organisation doit être dans le contexte")
                    .hasSize(1);
            assertThat(info.contexts().get(0).organizations().get(0).organizationId())
                    .isEqualTo(orgId);
        }

        @Test
        @DisplayName("refuse un token SSO invalide → erreur IllegalArgumentException")
        void shouldRejectInvalidSsoToken() {
            StepVerifier.create(sharedSessionService.userInfo("token.invalide.ici"))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }
    }
}
