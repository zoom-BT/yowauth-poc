package yowyob.comops.api.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import yowyob.comops.api.kernel.config.JwtTokenService;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires pour JwtTokenService — la fondation cryptographique de YowAuth0.
 *
 * Analogie : JwtTokenService est comme un notaire.
 * - Il signe des documents (émet des tokens JWT RS256)
 * - Il vérifie l'authenticité d'une signature (décode et valide les tokens)
 * - Il a une clé privée (pour signer) et une clé publique (pour vérifier)
 *
 * Un token JWT signé par ce service contient :
 *   - Un subject (qui est l'utilisateur ?)
 *   - Un issuer (qui a signé ?)
 *   - Une date d'expiration
 *   - Des claims personnalisés (tenantId, permissions, etc.)
 *
 * En production, la clé RSA est chargée depuis un fichier PEM.
 * En test, on utilise autoGenerateKeyPair=true (génération en mémoire).
 */
class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        SecurityRuntimeProperties props = new SecurityRuntimeProperties();
        props.getJwt().setAutoGenerateKeyPair(true);
        props.getJwt().setIssuer("yowyob-test");
        props.getJwt().setAccessTokenTtl(Duration.ofMinutes(15));
        jwtTokenService = new JwtTokenService(props);
    }

    // =========================================================================
    // État du service
    // =========================================================================

    @Nested
    @DisplayName("isEnabled() — état du service")
    class Enabled {

        @Test
        @DisplayName("retourne true quand autoGenerateKeyPair est activé")
        void shouldBeEnabledWithAutoGenerateKeyPair() {
            assertThat(jwtTokenService.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("retourne false quand aucune clé n'est configurée")
        void shouldBeDisabledWithNoKeyConfig() {
            SecurityRuntimeProperties unconfigured = new SecurityRuntimeProperties();
            // Ni privateKeyPath ni autoGenerateKeyPair → désactivé
            JwtTokenService disabledService = new JwtTokenService(unconfigured);

            assertThat(disabledService.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("lève IllegalStateException si on tente de signer sans clé configurée")
        void shouldThrowWhenSigningWithoutKey() {
            SecurityRuntimeProperties unconfigured = new SecurityRuntimeProperties();
            JwtTokenService disabledService = new JwtTokenService(unconfigured);

            assertThatThrownBy(() ->
                    disabledService.issueSignedToken("user-123", Duration.ofMinutes(5), Map.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not configured");
        }
    }

    // =========================================================================
    // Émission et vérification d'un token
    // =========================================================================

    @Nested
    @DisplayName("issueSignedToken() + decodeSignedToken() — signer et vérifier")
    class SignAndVerify {

        @Test
        @DisplayName("un token émis peut être décodé avec le bon subject")
        void shouldDecodeIssuedToken() {
            String userId = UUID.randomUUID().toString();

            String token = jwtTokenService.issueSignedToken(
                    userId, Duration.ofMinutes(5), Map.of("tid", "tenant-abc"));

            var decoded = jwtTokenService.decodeSignedToken(token);

            assertThat(decoded).isPresent();
            assertThat(decoded.get().getSubject())
                    .as("Le subject du token doit être l'userId")
                    .isEqualTo(userId);
        }

        @Test
        @DisplayName("les claims personnalisés sont bien embarqués dans le token")
        void shouldEmbedCustomClaims() {
            UUID tenantId = UUID.randomUUID();
            UUID orgId = UUID.randomUUID();

            String token = jwtTokenService.issueSignedToken(
                    "user-1",
                    Duration.ofMinutes(5),
                    Map.of(
                            "tid", tenantId.toString(),
                            "oid", orgId.toString(),
                            "svc", "SALES"));

            var claimsSet = jwtTokenService.decodeSignedToken(token).orElseThrow();

            // Récupération des claims personnalisés
            String extractedTid = getStringClaim(claimsSet, "tid");
            String extractedOid = getStringClaim(claimsSet, "oid");
            String extractedSvc = getStringClaim(claimsSet, "svc");

            assertThat(extractedTid).isEqualTo(tenantId.toString());
            assertThat(extractedOid).isEqualTo(orgId.toString());
            assertThat(extractedSvc).isEqualTo("SALES");
        }

        @Test
        @DisplayName("l'issuer du token correspond à celui configuré")
        void shouldEmbedCorrectIssuer() {
            String token = jwtTokenService.issueSignedToken(
                    "user-1", Duration.ofMinutes(5), Map.of());

            var claimsSet = jwtTokenService.decodeSignedToken(token).orElseThrow();

            assertThat(claimsSet.getIssuer())
                    .as("L'issuer doit correspondre à la configuration")
                    .isEqualTo("yowyob-test");
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token avec signature corrompue")
        void shouldRejectTamperedSignature() {
            String token = jwtTokenService.issueSignedToken(
                    "user-1", Duration.ofMinutes(5), Map.of());

            // On corrompt la signature (dernière partie du JWT après le deuxième '.')
            String[] parts = token.split("\\.");
            String tamperedToken = parts[0] + "." + parts[1] + ".SIGNATURE_INVALIDE";

            assertThat(jwtTokenService.decodeSignedToken(tamperedToken))
                    .as("Un token avec une signature corrompue doit être rejeté")
                    .isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token émis par un autre issuer")
        void shouldRejectTokenFromDifferentIssuer() {
            // Créer un second service avec un issuer différent
            SecurityRuntimeProperties otherProps = new SecurityRuntimeProperties();
            otherProps.getJwt().setAutoGenerateKeyPair(true);
            otherProps.getJwt().setIssuer("autre-service");
            JwtTokenService otherService = new JwtTokenService(otherProps);

            // Le token est signé par "autre-service" mais vérifié par "yowyob-test"
            // → rejeté car l'issuer ne correspond pas ET la clé est différente
            String foreignToken = otherService.issueSignedToken(
                    "user-1", Duration.ofMinutes(5), Map.of());

            assertThat(jwtTokenService.decodeSignedToken(foreignToken))
                    .as("Un token d'un autre issuer doit être rejeté")
                    .isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() pour un token null ou vide")
        void shouldRejectNullOrBlankToken() {
            assertThat(jwtTokenService.decodeSignedToken(null)).isEmpty();
            assertThat(jwtTokenService.decodeSignedToken("")).isEmpty();
            assertThat(jwtTokenService.decodeSignedToken("   ")).isEmpty();
        }
    }

    // =========================================================================
    // JWKS — clé publique exportée
    // =========================================================================

    @Nested
    @DisplayName("getPublicJwkSet() — export de la clé publique")
    class Jwks {

        @Test
        @DisplayName("retourne un JWKSet non vide contenant la clé publique RSA")
        void shouldReturnPublicJwkSet() {
            var jwkSet = jwtTokenService.getPublicJwkSet();

            assertThat(jwkSet).isNotNull();
            assertThat(jwkSet.getKeys())
                    .as("Le JWKSet doit contenir au moins une clé")
                    .isNotEmpty();
            // La clé doit être de type RSA
            assertThat(jwkSet.getKeys().get(0).getKeyType().getValue())
                    .as("La clé doit être de type RSA")
                    .isEqualTo("RSA");
        }

        @Test
        @DisplayName("la clé publique n'expose pas la clé privée")
        void publicJwkShouldNotExposePrivateKey() {
            var jwkSet = jwtTokenService.getPublicJwkSet();
            var key = jwkSet.getKeys().get(0);

            // La clé publique ne doit pas contenir de paramètre privé (d, p, q...)
            assertThat(key.isPrivate())
                    .as("La clé exportée dans le JWKS ne doit pas être une clé privée")
                    .isFalse();
        }
    }

    // =========================================================================
    // Méthode utilitaire
    // =========================================================================

    private String getStringClaim(com.nimbusds.jwt.JWTClaimsSet claimsSet, String claim) {
        try {
            return claimsSet.getStringClaim(claim);
        } catch (java.text.ParseException e) {
            return null;
        }
    }
}
