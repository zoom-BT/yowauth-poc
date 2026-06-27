package yowyob.comops.api.roles.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import yowyob.comops.api.roles.adapter.out.persistence.InMemoryRoleRepository;
import yowyob.comops.api.roles.adapter.out.persistence.InMemoryUserRoleAssignmentRepository;
import yowyob.comops.api.roles.application.port.in.AssignRoleToUserCommand;
import yowyob.comops.api.roles.application.port.in.CreateRoleCommand;
import yowyob.comops.api.roles.domain.model.Role;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 * Tests unitaires pour RolesPermissionResolver — le moteur RBAC.
 *
 * C'est le composant le plus important du module roles-core.
 * Son rôle : "Cet utilisateur a-t-il le droit de faire ça, à ce niveau ?"
 *
 * Analogie : c'est comme le contrôleur d'accès d'un immeuble.
 * - Un employé "TENANT" a accès à tous les étages (toute l'entreprise)
 * - Un employé "ORGANIZATION" n'a accès qu'à un seul bâtiment
 * - Un employé "AGENCY" n'a accès qu'à une seule salle
 *
 * Format des authorities générées :
 *   - Scope TENANT      → "READ_ORDERS"  ET  "READ_ORDERS#TENANT"
 *   - Scope ORGANIZATION → "READ_ORDERS#ORGANIZATION:<uuid-de-l-organisation>"
 *   - Scope AGENCY      → "READ_ORDERS#AGENCY:<uuid-de-l-agence>"
 *   - Scope SYSTEM      → "READ_ORDERS"  ET  "READ_ORDERS#SYSTEM"
 */
class RolesPermissionResolverTest {

    private InMemoryRoleRepository roleRepository;
    private InMemoryUserRoleAssignmentRepository assignmentRepository;
    private UserRoleAssignmentService assignmentService;
    private RoleApplicationService roleService;
    private RolesPermissionResolver resolver;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        roleRepository = new InMemoryRoleRepository();
        assignmentRepository = new InMemoryUserRoleAssignmentRepository();

        roleService = new RoleApplicationService(roleRepository, event -> Mono.empty());
        // Pas de cache de permissions dans les tests (Optional.empty())
        assignmentService = new UserRoleAssignmentService(assignmentRepository, Optional.empty(),
                event -> Mono.empty());
        resolver = new RolesPermissionResolver(assignmentRepository, roleRepository, Optional.empty());

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // =========================================================================
    // Résolution sans rôle
    // =========================================================================

    @Nested
    @DisplayName("Utilisateur sans aucun rôle")
    class NoRole {

        @Test
        @DisplayName("retourne un ensemble vide quand aucun rôle n'est attribué")
        void shouldReturnEmptySetForUserWithNoRole() {
            Set<String> permissions = resolver.resolvePermissions(tenantId, userId).block();

            assertThat(permissions)
                    .as("Un utilisateur sans rôle ne doit avoir aucune permission")
                    .isEmpty();
        }
    }

    // =========================================================================
    // Scope TENANT — accès à tout le tenant
    // =========================================================================

    @Nested
    @DisplayName("Rôle de scope TENANT")
    class TenantScope {

        @Test
        @DisplayName("génère la permission brute ET la permission suffixée #TENANT")
        void shouldGenerateBothRawAndSuffixedPermissions() {
            // 1. Créer un rôle TENANT avec une permission
            Role role = roleService.createRole(new CreateRoleCommand(
                    tenantId, "TENANT_ADMIN", "Tenant Admin",
                    "TENANT", Set.of("READ_ORDERS"))).block();

            // 2. Attribuer ce rôle à l'utilisateur au niveau TENANT
            assignmentService.assign(new AssignRoleToUserCommand(
                    tenantId, userId, role.id(), "TENANT")).block();

            // 3. Résoudre les permissions
            Set<String> permissions = resolver.resolvePermissions(tenantId, userId).block();

            // Pour un scope TENANT, on génère DEUX authorities :
            //   "READ_ORDERS"         → permission brute (accès global)
            //   "READ_ORDERS#TENANT"  → permission suffixée (pour filtrage précis)
            assertThat(permissions)
                    .as("Le scope TENANT doit générer la permission brute ET #TENANT")
                    .contains("READ_ORDERS", "READ_ORDERS#TENANT");
        }

        @Test
        @DisplayName("un rôle TENANT avec plusieurs permissions génère toutes les authorities")
        void shouldHandleMultiplePermissions() {
            Role role = roleService.createRole(new CreateRoleCommand(
                    tenantId, "MANAGER", "Manager",
                    "TENANT", Set.of("READ_ORDERS", "WRITE_ORDERS", "VIEW_REPORTS"))).block();

            assignmentService.assign(new AssignRoleToUserCommand(
                    tenantId, userId, role.id(), "TENANT")).block();

            Set<String> permissions = resolver.resolvePermissions(tenantId, userId).block();

            assertThat(permissions).contains(
                    "READ_ORDERS",   "READ_ORDERS#TENANT",
                    "WRITE_ORDERS",  "WRITE_ORDERS#TENANT",
                    "VIEW_REPORTS",  "VIEW_REPORTS#TENANT");
        }
    }

    // =========================================================================
    // Scope ORGANIZATION — accès limité à une organisation
    // =========================================================================

    @Nested
    @DisplayName("Rôle de scope ORGANIZATION")
    class OrganizationScope {

        @Test
        @DisplayName("génère la permission suffixée #ORGANIZATION:<uuid> uniquement")
        void shouldGenerateOrganizationScopedPermission() {
            UUID organizationId = UUID.randomUUID();

            Role role = roleService.createRole(new CreateRoleCommand(
                    tenantId, "ORG_MANAGER", "Org Manager",
                    "ORGANIZATION", Set.of("MANAGE_FLEET"))).block();

            // Attribuer dans le scope "ORGANIZATION:<uuid>"
            assignmentService.assign(new AssignRoleToUserCommand(
                    tenantId, userId, role.id(),
                    "ORGANIZATION:" + organizationId)).block();

            Set<String> permissions = resolver.resolvePermissions(tenantId, userId).block();

            String expectedAuthority = "MANAGE_FLEET#ORGANIZATION:" + organizationId;
            assertThat(permissions)
                    .as("Un rôle ORGANIZATION doit générer MANAGE_FLEET#ORGANIZATION:<uuid>")
                    .contains(expectedAuthority);

            // IMPORTANT : la permission brute sans scope NE doit PAS être présente
            // (l'accès est limité à cette organisation seulement)
            assertThat(permissions)
                    .as("La permission brute sans scope ne doit pas être présente pour un scope ORGANIZATION")
                    .doesNotContain("MANAGE_FLEET");
        }

        @Test
        @DisplayName("deux organisations différentes → deux authorities distinctes")
        void shouldDistinguishBetweenOrganizations() {
            UUID orgA = UUID.randomUUID();
            UUID orgB = UUID.randomUUID();

            Role role = roleService.createRole(new CreateRoleCommand(
                    tenantId, "ORG_VIEWER", "Org Viewer",
                    "ORGANIZATION", Set.of("VIEW_DATA"))).block();

            // L'utilisateur a le rôle dans ORG A et ORG B
            assignmentService.assign(new AssignRoleToUserCommand(
                    tenantId, userId, role.id(), "ORGANIZATION:" + orgA)).block();
            assignmentService.assign(new AssignRoleToUserCommand(
                    tenantId, userId, role.id(), "ORGANIZATION:" + orgB)).block();

            Set<String> permissions = resolver.resolvePermissions(tenantId, userId).block();

            assertThat(permissions)
                    .contains("VIEW_DATA#ORGANIZATION:" + orgA)
                    .contains("VIEW_DATA#ORGANIZATION:" + orgB);
        }
    }

    // =========================================================================
    // Scope AGENCY — accès limité à une agence
    // =========================================================================

    @Nested
    @DisplayName("Rôle de scope AGENCY")
    class AgencyScope {

        @Test
        @DisplayName("génère la permission suffixée #AGENCY:<uuid> uniquement")
        void shouldGenerateAgencyScopedPermission() {
            UUID agencyId = UUID.randomUUID();

            Role role = roleService.createRole(new CreateRoleCommand(
                    tenantId, "AGENT", "Agent",
                    "AGENCY", Set.of("PROCESS_BOOKING"))).block();

            assignmentService.assign(new AssignRoleToUserCommand(
                    tenantId, userId, role.id(),
                    "AGENCY", agencyId, null)).block();

            Set<String> permissions = resolver.resolvePermissions(tenantId, userId).block();

            assertThat(permissions)
                    .as("Un rôle AGENCY doit générer PROCESS_BOOKING#AGENCY:<uuid>")
                    .contains("PROCESS_BOOKING#AGENCY:" + agencyId);

            assertThat(permissions)
                    .as("La permission brute ne doit pas être présente pour un scope AGENCY")
                    .doesNotContain("PROCESS_BOOKING");
        }
    }

    // =========================================================================
    // Combinaison de rôles
    // =========================================================================

    @Nested
    @DisplayName("Utilisateur avec plusieurs rôles")
    class MultipleRoles {

        @Test
        @DisplayName("les permissions de tous les rôles sont fusionnées")
        void shouldMergePermissionsFromMultipleRoles() {
            UUID orgId = UUID.randomUUID();

            // Rôle 1 : TENANT niveau
            Role tenantRole = roleService.createRole(new CreateRoleCommand(
                    tenantId, "VIEWER", "Viewer",
                    "TENANT", Set.of("VIEW_DASHBOARD"))).block();

            // Rôle 2 : ORGANIZATION niveau
            Role orgRole = roleService.createRole(new CreateRoleCommand(
                    tenantId, "ORG_ADMIN", "Org Admin",
                    "ORGANIZATION", Set.of("MANAGE_MEMBERS"))).block();

            // Attribution des deux rôles au même utilisateur
            assignmentService.assign(new AssignRoleToUserCommand(
                    tenantId, userId, tenantRole.id(), "TENANT")).block();
            assignmentService.assign(new AssignRoleToUserCommand(
                    tenantId, userId, orgRole.id(), "ORGANIZATION:" + orgId)).block();

            Set<String> permissions = resolver.resolvePermissions(tenantId, userId).block();

            // Doit contenir les permissions du rôle TENANT
            assertThat(permissions).contains("VIEW_DASHBOARD", "VIEW_DASHBOARD#TENANT");
            // ET les permissions du rôle ORGANIZATION
            assertThat(permissions).contains("MANAGE_MEMBERS#ORGANIZATION:" + orgId);
        }
    }
}
