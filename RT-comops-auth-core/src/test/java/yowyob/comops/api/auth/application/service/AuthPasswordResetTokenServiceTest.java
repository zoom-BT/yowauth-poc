package yowyob.comops.api.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import yowyob.comops.api.auth.application.service.AuthPasswordResetTokenService.IssuedPasswordResetSelectionToken;
import yowyob.comops.api.auth.application.service.AuthPasswordResetTokenService.IssuedPasswordResetToken;
import yowyob.comops.api.auth.application.service.AuthPasswordResetTokenService.PasswordResetContext;
import yowyob.comops.api.auth.application.service.AuthPasswordResetTokenService.VerifiedPasswordResetSelectionToken;
import yowyob.comops.api.auth.application.service.AuthPasswordResetTokenService.VerifiedPasswordResetToken;
import yowyob.comops.api.auth.domain.model.UserAccount;
import yowyob.comops.api.kernel.config.JwtTokenService;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Tests unitaires pour AuthPasswordResetTokenService.
 *
 * Ce service gère les tokens du flux "Mot de passe oublié" :
 *
 * Étape 1 — Sélection du compte (multi-tenant possible) :
 *   issueSelection(principal, contexts)  → token de sélection (10 min)
 *   verifySelection(token)               → extrait les contextes disponibles
 *
 * Étape 2 — Réinitialisation effective :
 *   issueReset(userAccount)  → token de réinitialisation (15 min)
 *   verifyReset(token)       → extrait userId, tenantId, email
 *
 * Analogie : comme un formulaire de réinitialisation en deux étapes.
 *   1. Tu prouves que c'est bien ton adresse email (sélection)
 *   2. Tu reçois un lien temporaire pour changer ton mot de passe (reset)
 */
class AuthPasswordResetTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private AuthPasswordResetTokenService passwordResetService;

    @BeforeEach
    void setUp() {
        SecurityRuntimeProperties props = new SecurityRuntimeProperties();
        props.getJwt().setAutoGenerateKeyPair(true);
        props.getJwt().setIssuer("yowyob-test");
        jwtTokenService = new JwtTokenService(props);
        passwordResetService = new AuthPasswordResetTokenService(jwtTokenService);
    }

    /** Crée un contexte de réinitialisation fictif. */
    private PasswordResetContext makeContext(UUID tenantId, UUID userId) {
        return new PasswordResetContext(
                UUID.randomUUID().toString(),
                tenantId,
                userId,
                UUID.randomUUID(),
                "jean",
                "jean@example.com");
    }

    /** Crée un UserAccount en mémoire pour les tests de issueReset(). */
    private UserAccount makeAccount(UUID tenantId) {
        return UserAccount.register(
                tenantId, UUID.randomUUID(), "jean", "jean@example.com",
                new BCryptPasswordEncoder().encode("Passw0rd!secure"), "LOCAL");
    }

    // =========================================================================
    // issueSelection() + verifySelection() — étape 1 : sélection du compte
    // =========================================================================

    @Nested
    @DisplayName("issueSelection() — token de sélection multi-tenant")
    class IssueSelection {

        @Test
        @DisplayName("émet un token non vide avec une durée de vie positive")
        void shouldIssueNonBlankTokenWithPositiveTtl() {
            UUID tenantId = UUID.randomUUID();
            UUID userId   = UUID.randomUUID();

            IssuedPasswordResetSelectionToken issued = passwordResetService.issueSelection(
                    "jean@example.com",
                    List.of(makeContext(tenantId, userId)));

            assertThat(issued.token()).isNotBlank();
            assertThat(issued.expiresInSeconds())
                    .as("TTL de sélection : 10 minutes = 600 secondes")
                    .isEqualTo(600L);
        }

        @Test
        @DisplayName("refuse un principal null")
        void shouldRejectNullPrincipal() {
            assertThatThrownBy(() ->
                    passwordResetService.issueSelection(null, List.of(makeContext(UUID.randomUUID(), UUID.randomUUID()))))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("refuse une liste de contextes vide")
        void shouldRejectEmptyContextList() {
            assertThatThrownBy(() ->
                    passwordResetService.issueSelection("jean@example.com", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("verifySelection() — vérification du token de sélection")
    class VerifySelection {

        @Test
        @DisplayName("un token valide est vérifié et ses contextes sont extraits")
        void shouldVerifyValidSelectionTokenAndExtractContexts() {
            UUID tenantId = UUID.randomUUID();
            UUID userId   = UUID.randomUUID();
            PasswordResetContext context = makeContext(tenantId, userId);

            IssuedPasswordResetSelectionToken issued = passwordResetService.issueSelection(
                    "jean@example.com", List.of(context));

            Optional<VerifiedPasswordResetSelectionToken> verified =
                    passwordResetService.verifySelection(issued.token());

            assertThat(verified).isPresent();
            assertThat(verified.get().principal()).isEqualTo("jean@example.com");
            assertThat(verified.get().contexts()).hasSize(1);

            var extractedCtx = verified.get().contexts().get(0);
            assertThat(extractedCtx.tenantId()).isEqualTo(tenantId);
            assertThat(extractedCtx.userId()).isEqualTo(userId);
            assertThat(extractedCtx.email()).isEqualTo("jean@example.com");
            assertThat(extractedCtx.username()).isEqualTo("jean");
        }

        @Test
        @DisplayName("préserve tous les contextes multi-tenant dans le token")
        void shouldPreserveMultipleContexts() {
            UUID tenantA = UUID.randomUUID();
            UUID tenantB = UUID.randomUUID();
            UUID userA   = UUID.randomUUID();
            UUID userB   = UUID.randomUUID();

            IssuedPasswordResetSelectionToken issued = passwordResetService.issueSelection(
                    "jean@example.com",
                    List.of(makeContext(tenantA, userA), makeContext(tenantB, userB)));

            VerifiedPasswordResetSelectionToken verified =
                    passwordResetService.verifySelection(issued.token()).orElseThrow();

            assertThat(verified.contexts())
                    .as("Les deux contextes doivent être présents")
                    .hasSize(2);

            List<UUID> tenantIds = verified.contexts().stream()
                    .map(AuthPasswordResetTokenService.VerifiedPasswordResetContext::tenantId)
                    .toList();
            assertThat(tenantIds).contains(tenantA, tenantB);
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token corrompu")
        void shouldRejectTamperedToken() {
            IssuedPasswordResetSelectionToken issued = passwordResetService.issueSelection(
                    "jean@example.com", List.of(makeContext(UUID.randomUUID(), UUID.randomUUID())));

            String tampered = issued.token().substring(0, issued.token().length() - 8) + "CORROMPU";

            assertThat(passwordResetService.verifySelection(tampered)).isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token null ou vide")
        void shouldRejectNullOrBlankToken() {
            assertThat(passwordResetService.verifySelection(null)).isEmpty();
            assertThat(passwordResetService.verifySelection("")).isEmpty();
        }

        @Test
        @DisplayName("rejette un token de type 'auth-password-reset' (mauvais type)")
        void shouldRejectResetTokenAsSelectionToken() {
            // Un token de réinitialisation ne doit pas être accepté comme token de sélection
            UserAccount account = makeAccount(UUID.randomUUID());
            IssuedPasswordResetToken resetToken = passwordResetService.issueReset(account);

            assertThat(passwordResetService.verifySelection(resetToken.token()))
                    .as("Un token de type 'reset' ne doit pas être accepté comme token de sélection")
                    .isEmpty();
        }
    }

    // =========================================================================
    // issueReset() + verifyReset() — étape 2 : réinitialisation du mot de passe
    // =========================================================================

    @Nested
    @DisplayName("issueReset() — token de réinitialisation")
    class IssueReset {

        @Test
        @DisplayName("émet un token non vide avec une durée de vie de 15 minutes")
        void shouldIssueResetTokenWithCorrectTtl() {
            UserAccount account = makeAccount(UUID.randomUUID());

            IssuedPasswordResetToken issued = passwordResetService.issueReset(account);

            assertThat(issued.token()).isNotBlank();
            assertThat(issued.expiresInSeconds())
                    .as("TTL de réinitialisation : 15 minutes = 900 secondes")
                    .isEqualTo(900L);
        }
    }

    @Nested
    @DisplayName("verifyReset() — vérification du token de réinitialisation")
    class VerifyReset {

        @Test
        @DisplayName("un token valide est vérifié et ses données sont extraites")
        void shouldVerifyValidResetTokenAndExtractData() {
            UUID tenantId = UUID.randomUUID();
            UserAccount account = makeAccount(tenantId);

            IssuedPasswordResetToken issued = passwordResetService.issueReset(account);

            Optional<VerifiedPasswordResetToken> verified =
                    passwordResetService.verifyReset(issued.token());

            assertThat(verified).isPresent();
            assertThat(verified.get().tenantId()).isEqualTo(tenantId);
            assertThat(verified.get().userId()).isEqualTo(account.id());
            assertThat(verified.get().actorId()).isEqualTo(account.actorId());
            assertThat(verified.get().email()).isEqualTo("jean@example.com");
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token corrompu")
        void shouldRejectTamperedResetToken() {
            UserAccount account = makeAccount(UUID.randomUUID());
            IssuedPasswordResetToken issued = passwordResetService.issueReset(account);

            String tampered = issued.token().substring(0, issued.token().length() - 8) + "FALSIFIE";

            assertThat(passwordResetService.verifyReset(tampered)).isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token null ou vide")
        void shouldRejectNullOrBlankToken() {
            assertThat(passwordResetService.verifyReset(null)).isEmpty();
            assertThat(passwordResetService.verifyReset("")).isEmpty();
        }

        @Test
        @DisplayName("rejette un token de sélection comme token de réinitialisation (mauvais type)")
        void shouldRejectSelectionTokenAsResetToken() {
            IssuedPasswordResetSelectionToken selectionToken = passwordResetService.issueSelection(
                    "jean@example.com",
                    List.of(makeContext(UUID.randomUUID(), UUID.randomUUID())));

            assertThat(passwordResetService.verifyReset(selectionToken.token()))
                    .as("Un token de sélection ne doit pas être accepté comme token de réinitialisation")
                    .isEmpty();
        }
    }
}
