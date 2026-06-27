package yowyob.comops.api.kernel.config;

import com.nimbusds.jwt.JWTClaimsSet;
import java.text.ParseException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record JwtClaims(
        UUID userId,
        UUID tenantId,
        UUID organizationId,
        UUID agencyId,
        UUID actorId,
        Set<String> permissions,
        Instant issuedAt,
        Instant expiresAt,
        String jwtId,
        boolean mfaEnabled,
        boolean privilegedAdmin) {

    public static JwtClaims from(JWTClaimsSet claimsSet) {
        UUID userId = requireUuid(claimsSet.getSubject(), "sub");
        UUID tenantId = requireUuid(readStringClaim(claimsSet, "tid"), "tid");
        UUID actorId = parseUuidQuietly(readStringClaim(claimsSet, "actor"));
        UUID organizationId = parseUuidQuietly(readStringClaim(claimsSet, "oid"));
        UUID agencyId = parseUuidQuietly(readStringClaim(claimsSet, "aid"));
        Instant issuedAt = claimsSet.getIssueTime() == null ? null : claimsSet.getIssueTime().toInstant();
        Instant expiresAt = claimsSet.getExpirationTime() == null ? null : claimsSet.getExpirationTime().toInstant();
        boolean mfaEnabled = resolveBooleanClaim(claimsSet, "mfa");
        boolean privilegedAdmin = resolveBooleanClaim(claimsSet, "adm");
        return new JwtClaims(userId, tenantId, organizationId, agencyId, actorId,
                resolvePermissions(claimsSet), issuedAt, expiresAt, claimsSet.getJWTID(),
                mfaEnabled, privilegedAdmin);
    }

    private static String readStringClaim(JWTClaimsSet claimsSet, String claimName) {
        try {
            return claimsSet.getStringClaim(claimName);
        } catch (ParseException exception) {
            return null;
        }
    }

    private static UUID requireUuid(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required JWT claim '" + claimName + "' is missing.");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("JWT claim '" + claimName + "' is not a valid UUID.", exception);
        }
    }

    private static UUID parseUuidQuietly(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean resolveBooleanClaim(JWTClaimsSet claimsSet, String claimName) {
        Object raw = claimsSet.getClaim(claimName);
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw instanceof String value) {
            return Boolean.parseBoolean(value);
        }
        return false;
    }

    private static Set<String> resolvePermissions(JWTClaimsSet claimsSet) {
        Object raw = claimsSet.getClaim("permissions");
        if (!(raw instanceof List<?> list)) {
            return Set.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Object element : list) {
            if (element instanceof String value && !value.isBlank()) {
                values.add(value);
            }
        }
        return Set.copyOf(values);
    }
}
