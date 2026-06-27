package yowyob.comops.api.roles.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires pour le modèle de domaine Role.
 *
 * Analogie : un Role est comme une "fiche de poste" dans une entreprise.
 * - Le code est l'identifiant unique du poste (ex: "MANAGER_FLEET")
 * - Les permissions sont les actions autorisées (ex: "READ_VEHICLES", "ASSIGN_DRIVER")
 * - Le scopeType définit à quel niveau s'applique ce rôle (tenant entier, organisation, agence)
 *
 * L'objet est immuable : chaque modification produit une nouvelle "fiche de poste",
 * l'ancienne reste intacte — comme une nouvelle version de document.
 */
class RoleTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final Set<String> BASIC_PERMISSIONS = Set.of("READ_ORDERS", "VIEW_DASHBOARD");

    // =========================================================================
    // Création — Role.create()
    // =========================================================================

    @Nested
    @DisplayName("create() — création d'un nouveau rôle")
    class Create {

        @Test
        @DisplayName("crée un rôle avec les valeurs fournies")
        void shouldCreateRoleWithGivenValues() {
            Role role = Role.create(TENANT_ID, "MANAGER", "Manager Fleet", RoleScopeType.ORGANIZATION, BASIC_PERMISSIONS);

            assertThat(role.id()).isNotNull();
            assertThat(role.tenantId()).isEqualTo(TENANT_ID);
            assertThat(role.code()).isEqualTo("MANAGER");
            assertThat(role.name()).isEqualTo("Manager Fleet");
            assertThat(role.scopeType()).isEqualTo(RoleScopeType.ORGANIZATION);
            assertThat(role.permissions()).containsExactlyInAnyOrderElementsOf(BASIC_PERMISSIONS);
        }

        @Test
        @DisplayName("le scopeType par défaut est TENANT quand null est fourni")
        void shouldDefaultScopeTypeToTenant() {
            Role role = Role.create(TENANT_ID, "VIEWER", "Viewer", null, BASIC_PERMISSIONS);

            assertThat(role.scopeType())
                    .as("scopeType null doit devenir TENANT")
                    .isEqualTo(RoleScopeType.TENANT);
        }

        @Test
        @DisplayName("refuse un code vide")
        void shouldRejectBlankCode() {
            assertThatThrownBy(() ->
                    Role.create(TENANT_ID, "  ", "Some Role", RoleScopeType.TENANT, BASIC_PERMISSIONS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code");
        }

        @Test
        @DisplayName("refuse un nom vide")
        void shouldRejectBlankName() {
            assertThatThrownBy(() ->
                    Role.create(TENANT_ID, "CODE", "", RoleScopeType.TENANT, BASIC_PERMISSIONS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("refuse une liste de permissions vide")
        void shouldRejectEmptyPermissions() {
            assertThatThrownBy(() ->
                    Role.create(TENANT_ID, "CODE", "Name", RoleScopeType.TENANT, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("permissions");
        }

        @Test
        @DisplayName("deux appels à create() génèrent deux IDs différents")
        void shouldGenerateUniqueIds() {
            Role role1 = Role.create(TENANT_ID, "ROLE_A", "Role A", RoleScopeType.TENANT, BASIC_PERMISSIONS);
            Role role2 = Role.create(TENANT_ID, "ROLE_B", "Role B", RoleScopeType.TENANT, BASIC_PERMISSIONS);

            assertThat(role1.id()).isNotEqualTo(role2.id());
        }
    }

    // =========================================================================
    // Modification — rename() et replacePermissions()
    // =========================================================================

    @Nested
    @DisplayName("rename() et replacePermissions() — modifications immuables")
    class Mutations {

        @Test
        @DisplayName("rename() retourne un nouveau rôle avec le nom mis à jour")
        void shouldRenameRole() {
            Role original = Role.create(TENANT_ID, "ADMIN", "Administrateur", RoleScopeType.TENANT, BASIC_PERMISSIONS);

            Role renamed = original.rename("Super Administrateur");

            // Le nouveau objet a le nouveau nom
            assertThat(renamed.name()).isEqualTo("Super Administrateur");
            // L'original est intact
            assertThat(original.name()).isEqualTo("Administrateur");
            // L'ID ne change pas — c'est le même rôle
            assertThat(renamed.id()).isEqualTo(original.id());
        }

        @Test
        @DisplayName("replacePermissions() retourne un rôle avec les nouvelles permissions")
        void shouldReplacePermissions() {
            Role original = Role.create(TENANT_ID, "ADMIN", "Admin", RoleScopeType.TENANT, BASIC_PERMISSIONS);
            Set<String> newPermissions = Set.of("MANAGE_USERS", "DELETE_ACCOUNT", "EXPORT_DATA");

            Role updated = original.replacePermissions(newPermissions);

            assertThat(updated.permissions()).containsExactlyInAnyOrderElementsOf(newPermissions);
            // L'original est inchangé
            assertThat(original.permissions()).containsExactlyInAnyOrderElementsOf(BASIC_PERMISSIONS);
        }

        @Test
        @DisplayName("replacePermissions() refuse un ensemble vide")
        void shouldRejectEmptyReplacementPermissions() {
            Role role = Role.create(TENANT_ID, "ADMIN", "Admin", RoleScopeType.TENANT, BASIC_PERMISSIONS);

            assertThatThrownBy(() -> role.replacePermissions(Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
