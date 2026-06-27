package yowyob.comops.api.kernel.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private static final String AUDIENCE = "iwm-api";

    private final boolean enabled;
    private final RSAKey rsaKey;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtTokenService(SecurityRuntimeProperties securityRuntimeProperties) {
        SecurityRuntimeProperties.JwtProperties jwt = securityRuntimeProperties.getJwt();
        this.issuer = jwt.getIssuer();
        this.accessTokenTtl = resolveAccessTokenTtl(jwt.getAccessTokenTtl());
        if (securityRuntimeProperties.isJwtConfigured()) {
            this.enabled = true;
            this.rsaKey = initializeKeyPair(jwt);
        } else {
            this.enabled = false;
            this.rsaKey = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public String getIssuer() {
        return issuer;
    }

    public String issueAccessToken(
            UUID tenantId,
            UUID organizationId,
            UUID agencyId,
            UUID userId,
            UUID actorId,
            Set<String> permissions) {
        return issueAccessToken(tenantId, organizationId, agencyId, userId, actorId, permissions, false, false);
    }

    public String issueAccessToken(
            UUID tenantId,
            UUID organizationId,
            UUID agencyId,
            UUID userId,
            UUID actorId,
            Set<String> permissions,
            boolean mfaEnabled,
            boolean privilegedAdmin) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        Map<String, Object> additionalClaims = new java.util.LinkedHashMap<>();
        additionalClaims.put("tid", tenantId.toString());
        if (actorId != null) {
            additionalClaims.put("actor", actorId.toString());
        }
        if (organizationId != null) {
            additionalClaims.put("oid", organizationId.toString());
        }
        if (agencyId != null) {
            additionalClaims.put("aid", agencyId.toString());
        }
        if (permissions != null && !permissions.isEmpty()) {
            additionalClaims.put("permissions", new ArrayList<>(permissions));
        }
        additionalClaims.put("mfa", mfaEnabled);
        additionalClaims.put("adm", privilegedAdmin);
        return issueSignedToken(userId.toString(), accessTokenTtl, additionalClaims);
    }

    public String issueSignedToken(String subject, Duration ttl, Map<String, Object> additionalClaims) {
        if (!enabled) {
            throw new IllegalStateException("JWT signing is not configured.");
        }
        try {
            Instant now = Instant.now();
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer(issuer)
                    .audience(AUDIENCE)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(resolveAccessTokenTtl(ttl))))
                    .jwtID(UUID.randomUUID().toString());

            if (additionalClaims != null) {
                for (Map.Entry<String, Object> entry : additionalClaims.entrySet()) {
                    if (entry.getValue() != null) {
                        claims.claim(entry.getKey(), entry.getValue());
                    }
                }
            }

            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .build(), claims.build());
            jwt.sign(new RSASSASigner(rsaKey));
            return jwt.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to issue JWT access token.", exception);
        }
    }

    public Optional<JwtClaims> decode(String token) {
        return decodeSignedToken(token).map(JwtClaims::from);
    }

    public Optional<JWTClaimsSet> decodeSignedToken(String token) {
        if (!enabled || token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            RSAPublicKey publicKey = (RSAPublicKey) rsaKey.toPublicJWK().toPublicKey();
            if (!jwt.verify(new RSASSAVerifier(publicKey))) {
                return Optional.empty();
            }
            JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();
            if (claimsSet.getExpirationTime() == null
                    || claimsSet.getExpirationTime().toInstant().isBefore(Instant.now())) {
                return Optional.empty();
            }
            if (claimsSet.getIssuer() == null || !issuer.equals(claimsSet.getIssuer())) {
                return Optional.empty();
            }
            List<String> audience = claimsSet.getAudience();
            if (audience == null || !audience.contains(AUDIENCE)) {
                return Optional.empty();
            }
            return Optional.of(claimsSet);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public JWKSet getPublicJwkSet() {
        if (!enabled) {
            throw new IllegalStateException("JWT signing is not configured.");
        }
        return new JWKSet(List.of(rsaKey.toPublicJWK()));
    }

    /**
     * Dérive un HMAC-SHA256 d'une valeur à partir d'un secret connu du seul serveur (l'exposant
     * privé de la clé RSA). Sert à stocker dans un jeton signé l'empreinte d'un secret court (code
     * OTP, réponse captcha) SANS exposer le secret lui-même : le payload JWT est signé mais lisible,
     * y mettre le code en clair permettrait de le lire ; y mettre un simple hash permettrait un
     * brute-force hors-ligne (10^6 codes). Avec un HMAC à clé serveur, ni lecture ni brute-force
     * hors-ligne ne sont possibles. Stable en prod (clé chargée depuis le fichier secret).
     */
    public String deriveHmacSha256(String value) {
        if (!enabled) {
            throw new IllegalStateException("JWT signing is not configured.");
        }
        try {
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) rsaKey.toPrivateKey();
            byte[] keyMaterial = privateKey.getPrivateExponent().toByteArray();
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(keyMaterial, "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to derive HMAC-SHA256.", exception);
        }
    }

    private static Duration resolveAccessTokenTtl(Duration configured) {
        return configured == null || configured.isNegative() || configured.isZero()
                ? Duration.ofMinutes(15)
                : configured;
    }

    private static RSAKey initializeKeyPair(SecurityRuntimeProperties.JwtProperties jwt) {
        if (jwt.getPrivateKeyPath() != null && !jwt.getPrivateKeyPath().isBlank()) {
            return loadFromPkcs8Pem(jwt.getPrivateKeyPath(), jwt.getKeyId());
        }
        if (jwt.isAutoGenerateKeyPair()) {
            return generateKeyPairInMemory(jwt.getKeyId());
        }
        throw new IllegalStateException(
                "JWT key pair is not configured. Set 'iwm.security.jwt.private-key-path' or "
                        + "'iwm.security.jwt.auto-generate-key-pair=true'.");
    }

    private static RSAKey loadFromPkcs8Pem(String path, String keyId) {
        try {
            String pem = Files.readString(Path.of(path));
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyFactory
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory
                    .generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID(keyId)
                    .build();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read RSA private key PEM file: " + path, exception);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to parse RSA private key from PEM file: " + path
                            + ". Expected a PKCS#8 PEM private key.",
                    exception);
        }
    }

    private static RSAKey generateKeyPairInMemory(String keyId) {
        try {
            System.err.println(
                    "[IWM-WARN] iwm.security.jwt.auto-generate-key-pair=true. Development only; "
                            + "all JWT tokens become invalid after restart.");
            return new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID(keyId)
                    .generate();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to auto-generate RSA-2048 key pair.", exception);
        }
    }
}
