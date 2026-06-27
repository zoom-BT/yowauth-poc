package yowyob.comops.api.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import yowyob.comops.api.auth.adapter.out.persistence.InMemoryUserAccountRepository;
import yowyob.comops.api.auth.application.port.in.DiscoverLoginContextsCommand;
import yowyob.comops.api.auth.application.port.in.DiscoverLoginContextsResult;
import yowyob.comops.api.auth.application.port.in.SelectLoginContextCommand;
import yowyob.comops.api.auth.application.port.in.SelectedLoginContext;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccess;
import yowyob.comops.api.auth.domain.InvalidLoginCredentialsException;
import yowyob.comops.api.auth.domain.model.UserAccount;
import yowyob.comops.api.kernel.config.JwtTokenService;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Tests unitaires pour AuthContextApplicationService.
 *
 * Ce service implémente le cœur du flux de connexion YowAuth0 en deux étapes :
 *
 *   1. discover() — "Qui suis-je dans cet écosystème ?"
 *      L'utilisateur soumet email + mot de passe.
 *      Le service recherche tous ses comptes (multi-tenant), vérifie le mot de passe,
 *      et émet un token JWT contenant la liste des contextes disponibles.
 *
 *   2. select() — "Je choisis ce contexte"
 *      L'utilisateur choisit un contexte dans la liste.
 *      Le service vérifie le token, retrouve le contexte, et retourne le compte.
 *
 * Infrastructure de test :
 * - InMemoryUserAccountRepository : base de données en mémoire
 * - BCryptPasswordEncoder : même encodeur qu'en production
 * - JwtTokenService avec autoGenerateKeyPair=true : paire RSA générée en mémoire (tests uniquement)
 * - UserOrganizationAccessDirectory : lambda retournant Flux.empty() (pas d'orgs dans ces tests)
 * - RecordSystemAuditUseCase : lambda qui ignore les événements
 */
class AuthContextApplicationServiceTest {

    private static final String STRONG_PASSWORD = "Passw0rd!secure";

    private InMemoryUserAccountRepository userAccountRepository;
    private PasswordEncoder passwordEncoder;
    private AuthContextApplicationService service;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        userAccountRepository = new InMemoryUserAccountRepository();
        passwordEncoder = new BCryptPasswordEncoder();

        // JwtTokenService en mode "auto-generate" — génère une paire RSA-2048 en mémoire.
        // Parfait pour les tests : pas de fichier de clé, pas de config Spring nécessaire.
        SecurityRuntimeProperties props = new SecurityRuntimeProperties();
        props.getJwt().setAutoGenerateKeyPair(true);
        props.getJwt().setIssuer("test-issuer");
        jwtTokenService = new JwtTokenService(props);

        AuthContextSelectionTokenService tokenService = new AuthContextSelectionTokenService(jwtTokenService);

        service = new AuthContextApplicationService(
                userAccountRepository,
                passwordEncoder,
                // UserOrganizationAccessDirectory : interface à 1 méthode → lambda
                // On retourne Flux.empty() : l'utilisateur n'a pas d'organisations dans ces tests
                (tid, uid) -> Flux.empty(),
                tokenService,
                // RecordSystemAuditUseCase : on ignore les événements d'audit
                (tid, orgId, uid, action, type, ref, payload) -> reactor.core.publisher.Mono.empty()
        );
    }

    /**
     * Méthode utilitaire : crée et sauvegarde un compte directement en mémoire
     * (sans passer par AuthApplicationService pour éviter la dépendance au service complet).
     */
    private UserAccount createAccount(UUID tenantId, String username, String email) {
        UserAccount account = UserAccount.register(
                tenantId,
                UUID.randomUUID(),
                username,
                email,
                passwordEncoder.encode(STRONG_PASSWORD),
                "LOCAL");
        return userAccountRepository.save(account).block();
    }

    // =========================================================================
    // Étape 1 : discover() — découverte des contextes de connexion
    // =========================================================================

    @Nested
    @DisplayName("discover() — découverte des contextes multi-tenant")
    class Discover {

        @Test
        @DisplayName("retourne un contexte valide pour des identifiants corrects")
        void shouldReturnContextForValidCredentials() {
            UUID tenantId = UUID.randomUUID();
            createAccount(tenantId, "jean", "jean@example.com");

            DiscoverLoginContextsResult result = service.discover(
                    new DiscoverLoginContextsCommand("jean@example.com", STRONG_PASSWORD)).block();

            assertThat(result).isNotNull();
            // Un token de sélection JWT doit être émis
            assertThat(result.selectionToken())
                    .as("Un token de sélection doit être émis")
                    .isNotBlank();
            // Le token a une durée de vie positive
            assertThat(result.expiresInSeconds())
                    .as("Le token doit avoir une durée de vie > 0")
                    .isGreaterThan(0);
            // Un contexte trouvé (le compte dans tenantId)
            assertThat(result.contexts())
                    .as("Un contexte doit être retourné")
                    .hasSize(1);
            assertThat(result.contexts().get(0).tenantId())
                    .as("Le contexte doit appartenir au bon tenant")
                    .isEqualTo(tenantId);
        }

        @Test
        @DisplayName("retourne deux contextes quand le même email existe dans deux tenants")
        void shouldReturnTwoContextsForMultiTenantAccount() {
            // Analogie : jean@gmail.com travaille chez Fleetman ET chez BusStation
            UUID tenantFleetman = UUID.randomUUID();
            UUID tenantBusStation = UUID.randomUUID();
            createAccount(tenantFleetman, "jean", "jean@example.com");
            createAccount(tenantBusStation, "jean", "jean@example.com");

            DiscoverLoginContextsResult result = service.discover(
                    new DiscoverLoginContextsCommand("jean@example.com", STRONG_PASSWORD)).block();

            assertThat(result.contexts())
                    .as("Deux contextes doivent être retournés (un par tenant)")
                    .hasSize(2);

            // Les deux tenants doivent être présents
            List<UUID> tenantIds = result.contexts().stream()
                    .map(ctx -> ctx.tenantId())
                    .toList();
            assertThat(tenantIds)
                    .contains(tenantFleetman, tenantBusStation);
        }

        @Test
        @DisplayName("refuse avec InvalidLoginCredentialsException pour un mauvais mot de passe")
        void shouldRejectWrongPassword() {
            UUID tenantId = UUID.randomUUID();
            createAccount(tenantId, "jean", "jean@example.com");

            StepVerifier.create(service.discover(
                    new DiscoverLoginContextsCommand("jean@example.com", "WrongPassword1!")))
                    .expectError(InvalidLoginCredentialsException.class)
                    .verify();
        }

        @Test
        @DisplayName("refuse avec InvalidLoginCredentialsException pour un email inconnu")
        void shouldRejectUnknownPrincipal() {
            StepVerifier.create(service.discover(
                    new DiscoverLoginContextsCommand("nobody@example.com", STRONG_PASSWORD)))
                    .expectError(InvalidLoginCredentialsException.class)
                    .verify();
        }

        @Test
        @DisplayName("fonctionne avec le username à la place de l'email")
        void shouldAcceptUsernameAsPrincipal() {
            UUID tenantId = UUID.randomUUID();
            createAccount(tenantId, "jean.dupont", "jean@example.com");

            DiscoverLoginContextsResult result = service.discover(
                    new DiscoverLoginContextsCommand("jean.dupont", STRONG_PASSWORD)).block();

            assertThat(result.contexts()).hasSize(1);
        }
    }

    // =========================================================================
    // Étape 2 : select() — sélection d'un contexte de connexion
    // =========================================================================

    @Nested
    @DisplayName("select() — sélection du contexte de connexion")
    class Select {

        @Test
        @DisplayName("retourne le compte utilisateur pour un contextId valide")
        void shouldReturnUserAccountForValidContext() {
            UUID tenantId = UUID.randomUUID();
            UserAccount saved = createAccount(tenantId, "jean", "jean@example.com");

            // Étape 1 : discover pour obtenir le token et les contextIds
            DiscoverLoginContextsResult discovered = service.discover(
                    new DiscoverLoginContextsCommand("jean@example.com", STRONG_PASSWORD)).block();

            String contextId = discovered.contexts().get(0).contextId();
            String selectionToken = discovered.selectionToken();

            // Étape 2 : select avec le contextId trouvé
            SelectedLoginContext selected = service.select(
                    new SelectLoginContextCommand(selectionToken, contextId, null)).block();

            assertThat(selected).isNotNull();
            assertThat(selected.userAccount().id())
                    .as("Le compte retourné doit être le compte créé")
                    .isEqualTo(saved.id());
            assertThat(selected.userAccount().tenantId())
                    .isEqualTo(tenantId);
            assertThat(selected.organizationId())
                    .as("Aucune organisation sélectionnée → null")
                    .isNull();
        }

        @Test
        @DisplayName("sélectionne le bon contexte parmi deux dans un scénario multi-tenant")
        void shouldSelectCorrectContextInMultiTenantScenario() {
            UUID tenantA = UUID.randomUUID();
            UUID tenantB = UUID.randomUUID();
            UserAccount accountA = createAccount(tenantA, "jean", "jean@example.com");
            createAccount(tenantB, "jean", "jean@example.com");

            DiscoverLoginContextsResult discovered = service.discover(
                    new DiscoverLoginContextsCommand("jean@example.com", STRONG_PASSWORD)).block();

            // Trouver le contextId correspondant au tenant A
            String contextIdForA = discovered.contexts().stream()
                    .filter(ctx -> ctx.tenantId().equals(tenantA))
                    .findFirst()
                    .orElseThrow()
                    .contextId();

            SelectedLoginContext selected = service.select(
                    new SelectLoginContextCommand(discovered.selectionToken(), contextIdForA, null)).block();

            assertThat(selected.userAccount().id())
                    .as("Le compte sélectionné doit être celui du tenant A")
                    .isEqualTo(accountA.id());
        }

        @Test
        @DisplayName("refuse un token invalide (modifié) → IllegalArgumentException")
        void shouldRejectTamperedToken() {
            UUID tenantId = UUID.randomUUID();
            createAccount(tenantId, "jean", "jean@example.com");

            DiscoverLoginContextsResult discovered = service.discover(
                    new DiscoverLoginContextsCommand("jean@example.com", STRONG_PASSWORD)).block();
            String contextId = discovered.contexts().get(0).contextId();

            // Token délibérément corrompu
            String tamperedToken = discovered.selectionToken() + "INVALIDE";

            assertThatThrownBy(() -> service.select(
                    new SelectLoginContextCommand(tamperedToken, contextId, null)).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("token");
        }

        @Test
        @DisplayName("refuse un contextId inconnu → IllegalArgumentException")
        void shouldRejectUnknownContextId() {
            UUID tenantId = UUID.randomUUID();
            createAccount(tenantId, "jean", "jean@example.com");

            DiscoverLoginContextsResult discovered = service.discover(
                    new DiscoverLoginContextsCommand("jean@example.com", STRONG_PASSWORD)).block();

            assertThatThrownBy(() -> service.select(
                    new SelectLoginContextCommand(
                            discovered.selectionToken(),
                            "contexte-qui-nexiste-pas",
                            null)).block())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // Lien discover → select : le flux complet
    // =========================================================================

    @Nested
    @DisplayName("Flux complet discover → select")
    class FullFlow {

        @Test
        @DisplayName("le flux complet — discover puis select — retourne le bon utilisateur")
        void fullLoginFlowShouldWork() {
            // ARRANGE — un utilisateur avec accès à deux organisations différentes
            UUID tenantId = UUID.randomUUID();
            UUID orgId = UUID.randomUUID();

            // Service configuré avec un accès à une organisation (override du lambda)
            AuthContextSelectionTokenService tokenService = new AuthContextSelectionTokenService(jwtTokenService);
            AuthContextApplicationService serviceWithOrg = new AuthContextApplicationService(
                    userAccountRepository,
                    passwordEncoder,
                    // Cet utilisateur a accès à orgId
                    (tid, uid) -> Flux.just(
                            new UserOrganizationAccess(orgId, "ORG-001", "Mon Org", "Mon Organisation", List.of())),
                    tokenService,
                    (tid, oId, uid, action, type, ref, payload) -> reactor.core.publisher.Mono.empty()
            );

            UserAccount saved = createAccount(tenantId, "jean", "jean@example.com");

            // ACT — étape 1 : discover
            DiscoverLoginContextsResult discovered = serviceWithOrg.discover(
                    new DiscoverLoginContextsCommand("jean@example.com", STRONG_PASSWORD)).block();

            // ACT — étape 2 : select avec l'organisation
            String contextId = discovered.contexts().get(0).contextId();
            SelectedLoginContext selected = serviceWithOrg.select(
                    new SelectLoginContextCommand(discovered.selectionToken(), contextId, orgId)).block();

            // ASSERT — le bon compte avec l'organisation sélectionnée
            assertThat(selected.userAccount().id()).isEqualTo(saved.id());
            assertThat(selected.organizationId()).isEqualTo(orgId);
        }
    }
}
