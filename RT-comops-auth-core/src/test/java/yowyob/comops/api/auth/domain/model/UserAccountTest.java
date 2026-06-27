package yowyob.comops.api.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires pour UserAccount — le modèle de domaine du compte utilisateur.
 *
 * Ces tests ne nécessitent aucune base de données ni aucun framework.
 * Ils vérifient uniquement les règles métier définies dans la classe UserAccount :
 * - Les champs obligatoires
 * - Les normalisations (minuscule, majuscule)
 * - Les invariants du cahier de conception (DS-AU)
 * - Les transitions d'état (email vérifié, MFA, plan...)
 */
class UserAccountTest {

    // Données de base réutilisables dans tous les tests
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID  = UUID.randomUUID();

    // =========================================================================
    // Création d'un compte — register()
    // =========================================================================

    @Nested
    @DisplayName("register() — création d'un nouveau compte")
    class Register {

        @Test
        @DisplayName("crée un compte avec les valeurs par défaut correctes")
        void shouldCreateAccountWithCorrectDefaults() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean.dupont", "jean@example.com", "hashed_password", "LOCAL");

            // Identité
            assertThat(account.id()).isNotNull();
            assertThat(account.tenantId()).isEqualTo(TENANT_ID);
            assertThat(account.actorId()).isEqualTo(ACTOR_ID);

            // Champs normalisés
            assertThat(account.username()).isEqualTo("jean.dupont");   // déjà en minuscule
            assertThat(account.email()).isEqualTo("jean@example.com"); // déjà en minuscule
            assertThat(account.authProvider()).isEqualTo("LOCAL");     // en majuscule

            // Valeurs par défaut imposées par le domaine
            assertThat(account.status()).isEqualTo("ACTIVE");
            assertThat(account.plan()).isEqualTo("FREE_TIER");
            assertThat(account.onboardingStatus()).isEqualTo("NOT_STARTED");
            assertThat(account.onboardingStep()).isEqualTo(0);
            assertThat(account.accountType()).isEqualTo("PROSPECT");
            assertThat(account.mfaEnabled()).isFalse();
        }

        @Test
        @DisplayName("normalise le username en minuscule")
        void shouldNormalizeUsernameToLowercase() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "JEAN.DUPONT", "jean@example.com", "hash", "LOCAL");

            assertThat(account.username())
                    .as("Le username doit être en minuscule")
                    .isEqualTo("jean.dupont");
        }

        @Test
        @DisplayName("normalise l'email en minuscule")
        void shouldNormalizeEmailToLowercase() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "JEAN@EXAMPLE.COM", "hash", "LOCAL");

            assertThat(account.email())
                    .as("L'email doit être en minuscule")
                    .isEqualTo("jean@example.com");
        }

        @Test
        @DisplayName("refuse un username vide")
        void shouldRejectBlankUsername() {
            assertThatThrownBy(() ->
                    UserAccount.register(TENANT_ID, ACTOR_ID, "  ", "jean@example.com", "hash", "LOCAL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("username");
        }

        @Test
        @DisplayName("refuse un email vide")
        void shouldRejectBlankEmail() {
            assertThatThrownBy(() ->
                    UserAccount.register(TENANT_ID, ACTOR_ID, "jean", "", "hash", "LOCAL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("refuse un passwordHash vide")
        void shouldRejectBlankPasswordHash() {
            assertThatThrownBy(() ->
                    UserAccount.register(TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "   ", "LOCAL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("passwordHash");
        }

        @Test
        @DisplayName("refuse un authProvider vide")
        void shouldRejectBlankAuthProvider() {
            assertThatThrownBy(() ->
                    UserAccount.register(TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("authProvider");
        }

        @Test
        @DisplayName("l'email n'est pas vérifié à la création")
        void emailShouldNotBeVerifiedOnCreation() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            assertThat(account.emailVerified())
                    .as("L'email ne doit pas être vérifié à la création")
                    .isFalse();
            assertThat(account.emailVerifiedAt()).isNull();
        }

        @Test
        @DisplayName("le MFA est désactivé à la création")
        void mfaShouldBeDisabledOnCreation() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            assertThat(account.mfaEnabled()).isFalse();
            assertThat(account.mfaChannel()).isNull();
        }
    }

    // =========================================================================
    // Vérification email — markEmailVerified()
    // =========================================================================

    @Nested
    @DisplayName("markEmailVerified() — vérification de l'email")
    class MarkEmailVerified {

        @Test
        @DisplayName("marque l'email comme vérifié")
        void shouldMarkEmailAsVerified() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            UserAccount verified = account.markEmailVerified();

            assertThat(verified.emailVerified()).isTrue();
            assertThat(verified.emailVerifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("est idempotent — appeler deux fois ne change pas la date de vérification")
        void shouldBeIdempotent() {
            // Analogie : tamponner deux fois un document ne change pas la date du premier tampon
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            UserAccount firstVerification  = account.markEmailVerified();
            UserAccount secondVerification = firstVerification.markEmailVerified();

            assertThat(secondVerification.emailVerifiedAt())
                    .as("La date de vérification ne doit pas changer si l'email est déjà vérifié")
                    .isEqualTo(firstVerification.emailVerifiedAt());
        }
    }

    // =========================================================================
    // MFA — enableMfa() / disableMfa()
    // =========================================================================

    @Nested
    @DisplayName("MFA — activation et désactivation")
    class Mfa {

        @Test
        @DisplayName("enableMfa() active le MFA avec le bon canal")
        void shouldEnableMfaWithChannel() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            UserAccount withMfa = account.enableMfa("SMS");

            assertThat(withMfa.mfaEnabled()).isTrue();
            assertThat(withMfa.mfaChannel()).isEqualTo("SMS");
        }

        @Test
        @DisplayName("enableMfa() normalise le canal en majuscule")
        void shouldNormalizeMfaChannelToUppercase() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            UserAccount withMfa = account.enableMfa("email");

            assertThat(withMfa.mfaChannel()).isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("disableMfa() désactive le MFA et efface le canal")
        void shouldDisableMfa() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL")
                    .enableMfa("SMS");

            UserAccount withoutMfa = account.disableMfa();

            assertThat(withoutMfa.mfaEnabled()).isFalse();
            assertThat(withoutMfa.mfaChannel()).isNull();
        }

        @Test
        @DisplayName("enableMfa() refuse un canal vide")
        void shouldRejectBlankMfaChannel() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            assertThatThrownBy(() -> account.enableMfa(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mfaChannel");
        }
    }

    // =========================================================================
    // Plan et onboarding
    // =========================================================================

    @Nested
    @DisplayName("updatePlan() et updateOnboarding()")
    class PlanAndOnboarding {

        @Test
        @DisplayName("updatePlan() met à jour le plan")
        void shouldUpdatePlan() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            UserAccount updated = account.updatePlan("PREMIUM");

            assertThat(updated.plan()).isEqualTo("PREMIUM");
            // Les autres champs ne changent pas
            assertThat(updated.username()).isEqualTo("jean");
            assertThat(updated.email()).isEqualTo("jean@example.com");
        }

        @Test
        @DisplayName("updateOnboarding() met à jour l'étape et le statut")
        void shouldUpdateOnboarding() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            UserAccount updated = account.updateOnboarding(2, "IN_PROGRESS");

            assertThat(updated.onboardingStep()).isEqualTo(2);
            assertThat(updated.onboardingStatus()).isEqualTo("IN_PROGRESS");
        }
    }

    // =========================================================================
    // Liaison identité externe — linkExternalIdentity()
    // =========================================================================

    @Nested
    @DisplayName("linkExternalIdentity() — liaison avec un fournisseur externe")
    class LinkExternalIdentity {

        @Test
        @DisplayName("lie un provider externe (ex: GOOGLE) et son subject")
        void shouldLinkExternalProvider() {
            UserAccount account = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            // Imaginons que cet utilisateur lie son compte Google plus tard
            UserAccount linked = account.linkExternalIdentity("GOOGLE", "google-subject-12345");

            assertThat(linked.authProvider()).isEqualTo("GOOGLE");
            assertThat(linked.externalSubject()).isEqualTo("google-subject-12345");
        }
    }

    // =========================================================================
    // Invariants du cahier de conception
    // =========================================================================

    @Nested
    @DisplayName("Invariants — règles strictes du cahier de conception")
    class Invariants {

        @Test
        @DisplayName("onboardingStep négatif est refusé")
        void shouldRejectNegativeOnboardingStep() {
            // Le cahier dit : onboardingStep >= 0
            assertThatThrownBy(() ->
                    UserAccount.register(TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL")
                            .updateOnboarding(-1, "IN_PROGRESS"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deux appels à register() produisent deux IDs différents")
        void shouldGenerateUniqueIds() {
            UserAccount account1 = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean1", "jean1@example.com", "hash", "LOCAL");
            UserAccount account2 = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean2", "jean2@example.com", "hash", "LOCAL");

            assertThat(account1.id()).isNotEqualTo(account2.id());
        }

        @Test
        @DisplayName("le compte est immuable — register() retourne un nouvel objet sans modifier l'original")
        void shouldBeImmutable() {
            // Dans ce projet, UserAccount est immuable : chaque modification retourne
            // un NOUVEL objet. L'original n'est jamais modifié.
            // C'est une garantie de sécurité importante.
            UserAccount original = UserAccount.register(
                    TENANT_ID, ACTOR_ID, "jean", "jean@example.com", "hash", "LOCAL");

            UserAccount modified = original.updatePlan("PREMIUM");

            // L'original est intact
            assertThat(original.plan()).isEqualTo("FREE_TIER");
            // Le nouveau objet a la valeur mise à jour
            assertThat(modified.plan()).isEqualTo("PREMIUM");
        }
    }
}
