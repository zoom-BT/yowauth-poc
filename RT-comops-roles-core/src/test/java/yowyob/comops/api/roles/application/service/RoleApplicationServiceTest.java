package yowyob.comops.api.roles.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import yowyob.comops.api.roles.adapter.out.persistence.InMemoryRoleRepository;
import yowyob.comops.api.roles.application.port.in.CreateRoleCommand;
import yowyob.comops.api.roles.domain.DuplicateRoleCodeException;
import yowyob.comops.api.roles.domain.model.Role;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests unitaires pour RoleApplicationService.
 *
 * Ce service fait une seule chose : créer des rôles.
 * Mais il doit vérifier qu'on ne crée pas deux rôles avec le même code
 * dans le même tenant — sinon la base de données de droits serait incohérente.
 */
class RoleApplicationServiceTest {

    private InMemoryRoleRepository roleRepository;
    private RoleApplicationService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        roleRepository = new InMemoryRoleRepository();
        service = new RoleApplicationService(roleRepository, event -> Mono.empty());
        tenantId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("createRole() — création d'un rôle")
    class CreateRole {

        @Test
        @DisplayName("crée et sauvegarde un rôle avec les bonnes données")
        void shouldCreateAndSaveRole() {
            CreateRoleCommand command = new CreateRoleCommand(
                    tenantId, "FLEET_MANAGER", "Fleet Manager",
                    "ORGANIZATION",
                    Set.of("VIEW_VEHICLES", "ASSIGN_DRIVER"));

            Role saved = service.createRole(command).block();

            assertThat(saved).isNotNull();
            assertThat(saved.code()).isEqualTo("FLEET_MANAGER");
            assertThat(saved.name()).isEqualTo("Fleet Manager");
            assertThat(saved.permissions()).contains("VIEW_VEHICLES", "ASSIGN_DRIVER");

            // Vérification que c'est bien en mémoire
            Role fromRepo = roleRepository.findById(tenantId, saved.id()).block();
            assertThat(fromRepo).isNotNull();
        }

        @Test
        @DisplayName("refuse un code doublon dans le même tenant → DuplicateRoleCodeException")
        void shouldRejectDuplicateCodeInSameTenant() {
            service.createRole(new CreateRoleCommand(
                    tenantId, "ADMIN", "Admin", Set.of("ALL"))).block();

            StepVerifier.create(
                    service.createRole(new CreateRoleCommand(
                            tenantId, "ADMIN", "Admin Bis", Set.of("READ"))))
                    .expectError(DuplicateRoleCodeException.class)
                    .verify();
        }

        @Test
        @DisplayName("le même code dans deux tenants différents est autorisé")
        void shouldAllowSameCodeInDifferentTenants() {
            UUID tenantA = UUID.randomUUID();
            UUID tenantB = UUID.randomUUID();

            service.createRole(new CreateRoleCommand(tenantA, "ADMIN", "Admin A", Set.of("READ"))).block();
            Role inB = service.createRole(new CreateRoleCommand(tenantB, "ADMIN", "Admin B", Set.of("READ"))).block();

            assertThat(inB)
                    .as("Le même code 'ADMIN' doit pouvoir exister dans un tenant différent")
                    .isNotNull();
        }

        @Test
        @DisplayName("le scopeType null est accepté et devient TENANT par défaut")
        void shouldAcceptNullScopeType() {
            Role saved = service.createRole(new CreateRoleCommand(
                    tenantId, "VIEWER", "Viewer", Set.of("READ_ONLY"))).block();

            assertThat(saved.scopeType().name()).isEqualTo("TENANT");
        }
    }
}
