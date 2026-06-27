package yowyob.comops.api.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import yowyob.comops.api.auth.application.port.in.SelectableSignUpContext;
import yowyob.comops.api.auth.application.service.AuthSignUpSelectionTokenService.IssuedSignUpSelectionToken;
import yowyob.comops.api.auth.application.service.AuthSignUpSelectionTokenService.VerifiedSignUpSelectionToken;
import yowyob.comops.api.kernel.config.JwtTokenService;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires pour AuthSignUpSelectionTokenService.
 *
 * Ce service gère les tokens du flux "Inscription avec code organisation" :
 *
 * Séquence :
 *   1. Un employé entre le code de son entreprise (ex: "YOWYOB-2024")
 *   2. Le système trouve les organisations correspondantes → issue(orgCode, contexts)
 *   3. L'employé sélectionne son organisation → verify(token)
 *   4. L'inscription est finalisée pour ce contexte
 *
 * Analogie : c'est comme un code d'invitation d'entreprise.
 * Tu reçois un lien "Rejoignez Fleetman avec le code FLEET-2024",
 * tu cliques, tu choisis ton rôle, et tu t'inscris directement dans
 * l'espace de ta société.
 */
class AuthSignUpSelectionTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private AuthSignUpSelectionTokenService signUpSelectionService;

    @BeforeEach
    void setUp() {
        SecurityRuntimeProperties props = new SecurityRuntimeProperties();
        props.getJwt().setAutoGenerateKeyPair(true);
        props.getJwt().setIssuer("yowyob-test");
        jwtTokenService = new JwtTokenService(props);
        signUpSelectionService = new AuthSignUpSelectionTokenService(jwtTokenService);
    }

    /** Crée un contexte d'inscription fictif. */
    private SelectableSignUpContext makeContext(UUID tenantId, UUID orgId,
                                               String orgCode, String orgName) {
        return new SelectableSignUpContext(
                UUID.randomUUID().toString(),
                tenantId,
                orgId,
                orgCode,
                orgName,
                "ENTERPRISE");
    }

    // =========================================================================
    // issue() — émission du token de sélection d'inscription
    // =========================================================================

    @Nested
    @DisplayName("issue() — émission du token de sélection")
    class Issue {

        @Test
        @DisplayName("émet un token non vide avec une durée de vie de 10 minutes")
        void shouldIssueTokenWithCorrectTtl() {
            UUID tenantId = UUID.randomUUID();
            UUID orgId    = UUID.randomUUID();

            IssuedSignUpSelectionToken issued = signUpSelectionService.issue(
                    "YOWYOB-2024",
                    List.of(makeContext(tenantId, orgId, "YOWYOB-2024", "Yowyob SA")));

            assertThat(issued.token()).isNotBlank();
            assertThat(issued.expiresInSeconds())
                    .as("TTL : 10 minutes = 600 secondes")
                    .isEqualTo(600L);
        }

        @Test
        @DisplayName("le code organisation est normalisé en majuscules")
        void shouldNormalizeOrganizationCodeToUpperCase() {
            UUID tenantId = UUID.randomUUID();
            UUID orgId    = UUID.randomUUID();

            IssuedSignUpSelectionToken issued = signUpSelectionService.issue(
                    "yowyob-2024",  // minuscules
                    List.of(makeContext(tenantId, orgId, "yowyob-2024", "Yowyob SA")));

            VerifiedSignUpSelectionToken verified =
                    signUpSelectionService.verify(issued.token()).orElseThrow();

            assertThat(verified.organizationCode())
                    .as("Le code doit être normalisé en majuscules dans le token")
                    .isEqualTo("YOWYOB-2024");
        }

        @Test
        @DisplayName("refuse un organizationCode null")
        void shouldRejectNullOrganizationCode() {
            assertThatThrownBy(() ->
                    signUpSelectionService.issue(
                            null,
                            List.of(makeContext(UUID.randomUUID(), UUID.randomUUID(), "CODE", "Org"))))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("refuse une liste de contextes vide")
        void shouldRejectEmptyContextList() {
            assertThatThrownBy(() ->
                    signUpSelectionService.issue("YOWYOB-2024", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // verify() — vérification du token de sélection
    // =========================================================================

    @Nested
    @DisplayName("verify() — vérification et décodage du token")
    class Verify {

        @Test
        @DisplayName("un token valide est vérifié et ses contextes sont extraits")
        void shouldVerifyValidTokenAndExtractContexts() {
            UUID tenantId = UUID.randomUUID();
            UUID orgId    = UUID.randomUUID();
            SelectableSignUpContext context =
                    makeContext(tenantId, orgId, "FLEET-2024", "Fleetman Corp");

            IssuedSignUpSelectionToken issued = signUpSelectionService.issue(
                    "FLEET-2024", List.of(context));

            Optional<VerifiedSignUpSelectionToken> verified =
                    signUpSelectionService.verify(issued.token());

            assertThat(verified).isPresent();
            assertThat(verified.get().organizationCode()).isEqualTo("FLEET-2024");
            assertThat(verified.get().contexts()).hasSize(1);

            var extractedCtx = verified.get().contexts().get(0);
            assertThat(extractedCtx.tenantId()).isEqualTo(tenantId);
            assertThat(extractedCtx.organizationId()).isEqualTo(orgId);
            assertThat(extractedCtx.organizationCode()).isEqualTo("FLEET-2024");
            assertThat(extractedCtx.organizationName()).isEqualTo("Fleetman Corp");
            assertThat(extractedCtx.organizationType()).isEqualTo("ENTERPRISE");
        }

        @Test
        @DisplayName("préserve plusieurs contextes dans le token")
        void shouldPreserveMultipleContexts() {
            UUID tenantA = UUID.randomUUID();
            UUID tenantB = UUID.randomUUID();
            UUID orgA    = UUID.randomUUID();
            UUID orgB    = UUID.randomUUID();

            IssuedSignUpSelectionToken issued = signUpSelectionService.issue(
                    "GROUPE-X",
                    List.of(
                            makeContext(tenantA, orgA, "GROUPE-X", "Groupe X Paris"),
                            makeContext(tenantB, orgB, "GROUPE-X", "Groupe X Lyon")));

            VerifiedSignUpSelectionToken verified =
                    signUpSelectionService.verify(issued.token()).orElseThrow();

            assertThat(verified.contexts())
                    .as("Les deux contextes doivent être présents dans le token")
                    .hasSize(2);

            List<UUID> tenantIds = verified.contexts().stream()
                    .map(AuthSignUpSelectionTokenService.VerifiedSelectableSignUpContext::tenantId)
                    .toList();
            assertThat(tenantIds).contains(tenantA, tenantB);
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token corrompu")
        void shouldRejectTamperedToken() {
            IssuedSignUpSelectionToken issued = signUpSelectionService.issue(
                    "CODE-123",
                    List.of(makeContext(UUID.randomUUID(), UUID.randomUUID(), "CODE-123", "Org")));

            String tampered = issued.token().substring(0, issued.token().length() - 8) + "CORROMPU";

            assertThat(signUpSelectionService.verify(tampered)).isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token null ou vide")
        void shouldRejectNullOrBlankToken() {
            assertThat(signUpSelectionService.verify(null)).isEmpty();
            assertThat(signUpSelectionService.verify("")).isEmpty();
        }

        @Test
        @DisplayName("rejette un token d'un autre type (password reset selection)")
        void shouldRejectTokenFromAnotherService() {
            // Un token de reset ne doit pas être accepté comme token d'inscription
            AuthPasswordResetTokenService otherService =
                    new AuthPasswordResetTokenService(jwtTokenService);

            AuthPasswordResetTokenService.PasswordResetContext ctx =
                    new AuthPasswordResetTokenService.PasswordResetContext(
                            UUID.randomUUID().toString(),
                            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                            "jean", "jean@example.com");

            String foreignToken = otherService.issueSelection("jean@example.com", List.of(ctx)).token();

            assertThat(signUpSelectionService.verify(foreignToken))
                    .as("Un token de password-reset-selection ne doit pas être accepté ici")
                    .isEmpty();
        }
    }
}
