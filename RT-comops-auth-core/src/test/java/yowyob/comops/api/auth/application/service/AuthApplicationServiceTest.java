package yowyob.comops.api.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import yowyob.comops.api.auth.adapter.out.persistence.InMemoryUserAccountRepository;
import yowyob.comops.api.auth.application.port.in.IdentifyAccountCommand;
import yowyob.comops.api.auth.application.port.in.IdentifyAccountResult;
import yowyob.comops.api.auth.application.port.in.LoginCommand;
import yowyob.comops.api.auth.application.port.in.RegisterUserCommand;
import yowyob.comops.api.auth.domain.DuplicateEmailException;
import yowyob.comops.api.auth.domain.DuplicateUsernameException;
import yowyob.comops.api.auth.domain.InvalidLoginCredentialsException;
import yowyob.comops.api.auth.domain.model.UserAccount;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.test.StepVerifier;

/**
 * Tests unitaires pour AuthApplicationService.
 *
 * Analogie : penser à AuthApplicationService comme au guichet d'une banque.
 * - register()  → le conseiller crée un compte pour un nouveau client
 * - login()     → le client prouve son identité pour accéder à son compte
 * - identify()  → le guichetier vérifie si le client existe déjà ou non
 *
 * On utilise :
 * - InMemoryUserAccountRepository : simule la base de données (rien sur disque)
 * - BCryptPasswordEncoder          : même encodeur que la production
 * - lambda pour RecordSystemAuditUseCase : on ignore l'audit dans les tests
 * - null pour les services token/email   : jamais appelés par ces 3 méthodes
 */
class AuthApplicationServiceTest {

    // Mot de passe valide réutilisable (respecte la règle : 10 chars, maj, min, chiffre, symbole)
    private static final String STRONG_PASSWORD = "Passw0rd!secure";

    private InMemoryUserAccountRepository userAccountRepository;
    private AuthApplicationService service;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        userAccountRepository = new InMemoryUserAccountRepository();

        service = new AuthApplicationService(
                userAccountRepository,
                new BCryptPasswordEncoder(),
                // RecordSystemAuditUseCase : 1 seule méthode → lambda possible
                (tid, orgId, userId, action, type, ref, payload) -> reactor.core.publisher.Mono.empty(),
                // BusinessEventPublisher : ignoré dans ces tests unitaires
                event -> reactor.core.publisher.Mono.empty(),
                // CreateActorUseCase : non appelée par register/login/identify → null safe
                null,
                // SignUpContextDirectory : non utilisée ici
                null,
                // AuthSignUpSelectionTokenService, AuthPasswordResetTokenService,
                // AuthEmailVerificationTokenService, AuthEmailDeliveryService,
                // AuthChallengeTokenService : toutes null — non appelées dans nos tests
                null, null, null, null, null,
                // TenantOwnerRoleProvisioner : no-op dans ces tests unitaires
                (tid, userId) -> reactor.core.publisher.Mono.empty()
        );

        tenantId = UUID.randomUUID();
    }

    // =========================================================================
    // DS-A-01 : Enregistrement d'un compte utilisateur — register()
    // =========================================================================

    @Nested
    @DisplayName("register() — création administrative d'un compte")
    class Register {

        @Test
        @DisplayName("crée un compte et le sauvegarde en base")
        void shouldCreateAndSaveAccount() {
            RegisterUserCommand command = new RegisterUserCommand(
                    tenantId, UUID.randomUUID(),
                    "jean.dupont", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL");

            UserAccount saved = service.register(command).block();

            assertThat(saved).isNotNull();
            assertThat(saved.username()).isEqualTo("jean.dupont");
            assertThat(saved.email()).isEqualTo("jean@example.com");
            assertThat(saved.authProvider()).isEqualTo("LOCAL");
            assertThat(saved.tenantId()).isEqualTo(tenantId);

            // Vérification que c'est bien en mémoire
            UserAccount fromRepo = userAccountRepository
                    .findByPrincipal(tenantId, "jean.dupont")
                    .block();
            assertThat(fromRepo).isNotNull();
            assertThat(fromRepo.id()).isEqualTo(saved.id());
        }

        @Test
        @DisplayName("le mot de passe est haché (jamais stocké en clair)")
        void passwordShouldBeHashed() {
            RegisterUserCommand command = new RegisterUserCommand(
                    tenantId, UUID.randomUUID(),
                    "jean", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL");

            UserAccount saved = service.register(command).block();

            // Le hash ne doit pas être égal au mot de passe original
            assertThat(saved.passwordHash())
                    .as("Le mot de passe ne doit jamais être stocké en clair")
                    .isNotEqualTo(STRONG_PASSWORD);

            // Mais BCrypt doit confirmer qu'ils correspondent
            assertThat(new BCryptPasswordEncoder().matches(STRONG_PASSWORD, saved.passwordHash()))
                    .as("BCrypt doit pouvoir vérifier le mot de passe")
                    .isTrue();
        }

        @Test
        @DisplayName("refuse si le username est déjà utilisé → DuplicateUsernameException")
        void shouldRejectDuplicateUsername() {
            // Premier enregistrement (OK)
            service.register(new RegisterUserCommand(
                    tenantId, UUID.randomUUID(),
                    "jean", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();

            // Deuxième avec le même username
            StepVerifier.create(
                    service.register(new RegisterUserCommand(
                            tenantId, UUID.randomUUID(),
                            "jean",              // <-- doublon
                            "autre@example.com",
                            STRONG_PASSWORD, "LOCAL")))
                    .expectError(DuplicateUsernameException.class)
                    .verify();
        }

        @Test
        @DisplayName("refuse si l'email est déjà utilisé → DuplicateEmailException")
        void shouldRejectDuplicateEmail() {
            // Premier enregistrement (OK)
            service.register(new RegisterUserCommand(
                    tenantId, UUID.randomUUID(),
                    "jean", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();

            // Deuxième avec le même email
            StepVerifier.create(
                    service.register(new RegisterUserCommand(
                            tenantId, UUID.randomUUID(),
                            "autre.user",
                            "jean@example.com",  // <-- doublon
                            STRONG_PASSWORD, "LOCAL")))
                    .expectError(DuplicateEmailException.class)
                    .verify();
        }

        @Test
        @DisplayName("refuse un mot de passe trop faible (moins de 10 caractères)")
        void shouldRejectWeakPassword_TooShort() {
            // La validation est synchrone : l'exception est levée avant même que le Mono soit créé
            assertThatThrownBy(() ->
                    service.register(new RegisterUserCommand(
                            tenantId, UUID.randomUUID(),
                            "jean", "jean@example.com",
                            "Court1!",  // 7 caractères, trop court
                            "LOCAL")).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("password");
        }

        @Test
        @DisplayName("refuse un mot de passe sans majuscule")
        void shouldRejectWeakPassword_NoUppercase() {
            assertThatThrownBy(() ->
                    service.register(new RegisterUserCommand(
                            tenantId, UUID.randomUUID(),
                            "jean", "jean@example.com",
                            "passw0rd!secure",  // pas de majuscule
                            "LOCAL")).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("password");
        }

        @Test
        @DisplayName("un même email est accepté dans deux tenants différents")
        void sameEmailInDifferentTenantsShouldBeAllowed() {
            UUID tenantA = UUID.randomUUID();
            UUID tenantB = UUID.randomUUID();

            // Même email, tenant A
            service.register(new RegisterUserCommand(
                    tenantA, UUID.randomUUID(),
                    "jean", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();

            // Même email, tenant B → doit réussir
            UserAccount inTenantB = service.register(new RegisterUserCommand(
                    tenantB, UUID.randomUUID(),
                    "jean", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();

            assertThat(inTenantB)
                    .as("Le même email doit être accepté dans un tenant différent")
                    .isNotNull();
        }
    }

    // =========================================================================
    // DS-A-02 : Connexion — login()
    // =========================================================================

    @Nested
    @DisplayName("login() — connexion par identifiant/mot de passe")
    class Login {

        @Test
        @DisplayName("connecte un utilisateur avec son username")
        void shouldLoginWithUsername() {
            // Créer le compte d'abord
            UserAccount registered = service.register(new RegisterUserCommand(
                    tenantId, UUID.randomUUID(),
                    "jean.dupont", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();
            // Mode strict : email à vérifier avant de pouvoir se connecter.
            userAccountRepository.save(registered.markEmailVerified()).block();

            // Se connecter avec le username
            UserAccount loggedIn = service.login(
                    new LoginCommand(tenantId, "jean.dupont", STRONG_PASSWORD)).block();

            assertThat(loggedIn).isNotNull();
            assertThat(loggedIn.username()).isEqualTo("jean.dupont");
        }

        @Test
        @DisplayName("connecte un utilisateur avec son email")
        void shouldLoginWithEmail() {
            UserAccount registered = service.register(new RegisterUserCommand(
                    tenantId, UUID.randomUUID(),
                    "jean.dupont", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();
            // Mode strict : email à vérifier avant de pouvoir se connecter.
            userAccountRepository.save(registered.markEmailVerified()).block();

            // Se connecter avec l'email cette fois
            UserAccount loggedIn = service.login(
                    new LoginCommand(tenantId, "jean@example.com", STRONG_PASSWORD)).block();

            assertThat(loggedIn).isNotNull();
            assertThat(loggedIn.email()).isEqualTo("jean@example.com");
        }

        @Test
        @DisplayName("refuse la connexion d'un compte LOCAL dont l'email n'est pas vérifié (mode strict)")
        void shouldRejectLoginWhenEmailNotVerified() {
            // Compte LOCAL non vérifié créé directement (service.register auto-vérifie, donc on
            // contourne pour reproduire un compte issu d'un sign-up self-service non confirmé).
            UserAccount unverified = UserAccount.register(
                    tenantId, UUID.randomUUID(),
                    "jane.doe", "jane@example.com",
                    new BCryptPasswordEncoder().encode(STRONG_PASSWORD), "LOCAL");
            userAccountRepository.save(unverified).block();

            assertThatThrownBy(() -> service.login(
                    new LoginCommand(tenantId, "jane@example.com", STRONG_PASSWORD)).block())
                    .isInstanceOf(yowyob.comops.api.auth.domain.EmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("refuse un mauvais mot de passe → InvalidLoginCredentialsException")
        void shouldRejectWrongPassword() {
            service.register(new RegisterUserCommand(
                    tenantId, UUID.randomUUID(),
                    "jean", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();

            StepVerifier.create(
                    service.login(new LoginCommand(tenantId, "jean", "WrongPassword1!")))
                    .expectError(InvalidLoginCredentialsException.class)
                    .verify();
        }

        @Test
        @DisplayName("refuse un utilisateur inexistant → InvalidLoginCredentialsException")
        void shouldRejectUnknownUser() {
            StepVerifier.create(
                    service.login(new LoginCommand(tenantId, "nobody", STRONG_PASSWORD)))
                    .expectError(InvalidLoginCredentialsException.class)
                    .verify();
        }

        @Test
        @DisplayName("le login est sensible au tenant — le compte d'un autre tenant n'est pas trouvé")
        void loginShouldBeTenantScoped() {
            UUID otherTenantId = UUID.randomUUID();

            // Compte créé dans otherTenantId
            service.register(new RegisterUserCommand(
                    otherTenantId, UUID.randomUUID(),
                    "jean", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();

            // Tentative de login dans tenantId (différent) → doit échouer
            StepVerifier.create(
                    service.login(new LoginCommand(tenantId, "jean", STRONG_PASSWORD)))
                    .expectError(InvalidLoginCredentialsException.class)
                    .verify();
        }
    }

    // =========================================================================
    // identify() — "Qui es-tu ?" — navigation pré-login
    // =========================================================================

    @Nested
    @DisplayName("identify() — découverte de l'action appropriée pour un principal")
    class Identify {

        @Test
        @DisplayName("retourne SIGN_UP quand aucun compte ne correspond au principal")
        void shouldReturnSignUpForUnknownPrincipal() {
            IdentifyAccountResult result = service.identify(
                    new IdentifyAccountCommand("nobody@example.com")).block();

            assertThat(result.accountExists()).isFalse();
            assertThat(result.nextStep()).isEqualTo("SIGN_UP");
            assertThat(result.matchingAccountCount()).isZero();
        }

        @Test
        @DisplayName("retourne SIGN_IN_PASSWORD quand un compte existe pour ce principal")
        void shouldReturnSignInPasswordForKnownPrincipal() {
            // Créer un compte (dans un tenant quelconque)
            service.register(new RegisterUserCommand(
                    tenantId, UUID.randomUUID(),
                    "jean", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();

            // identify() cherche tous tenants confondus
            IdentifyAccountResult result = service.identify(
                    new IdentifyAccountCommand("jean@example.com")).block();

            assertThat(result.accountExists()).isTrue();
            assertThat(result.nextStep()).isEqualTo("SIGN_IN_PASSWORD");
            assertThat(result.matchingAccountCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("compte plusieurs comptes quand le même email existe dans plusieurs tenants")
        void shouldCountAccountsAcrossTenants() {
            UUID tenantA = UUID.randomUUID();
            UUID tenantB = UUID.randomUUID();

            // Même email dans 2 tenants différents
            service.register(new RegisterUserCommand(
                    tenantA, UUID.randomUUID(),
                    "jean", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();
            service.register(new RegisterUserCommand(
                    tenantB, UUID.randomUUID(),
                    "jean", "jean@example.com",
                    STRONG_PASSWORD, "LOCAL")).block();

            IdentifyAccountResult result = service.identify(
                    new IdentifyAccountCommand("jean@example.com")).block();

            assertThat(result.matchingAccountCount())
                    .as("identify() doit trouver les comptes dans tous les tenants")
                    .isEqualTo(2L);
        }
    }
}
