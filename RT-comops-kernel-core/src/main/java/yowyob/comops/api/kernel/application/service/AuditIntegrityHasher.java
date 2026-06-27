package yowyob.comops.api.kernel.application.service;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import yowyob.comops.api.kernel.config.AuditIntegrityProperties;
import yowyob.comops.api.kernel.domain.model.SystemAuditEntry;

@Component
public class AuditIntegrityHasher {

    private static final Logger LOG = LoggerFactory.getLogger(AuditIntegrityHasher.class);
    private static final String ALGO = "HmacSHA256";

    private final AuditIntegrityProperties properties;
    private final byte[] keyMaterial;
    private final boolean active;

    public AuditIntegrityHasher(AuditIntegrityProperties properties) {
        this.properties = properties;
        if (properties.isEnabled() && properties.getHmacSecret() != null && !properties.getHmacSecret().isBlank()) {
            this.keyMaterial = properties.getHmacSecret().getBytes(StandardCharsets.UTF_8);
            this.active = true;
            if (this.keyMaterial.length < 32) {
                LOG.warn("iwm.security.audit-integrity.hmac-secret is shorter than 32 bytes; "
                        + "use a 256-bit random secret for production.");
            }
        } else {
            this.keyMaterial = null;
            this.active = false;
            if (properties.isEnabled()) {
                LOG.warn("Audit integrity hashing is enabled but no HMAC secret is configured. "
                        + "Set iwm.security.audit-integrity.hmac-secret to enable tamper-evident audit.");
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public String hash(SystemAuditEntry entry) {
        if (!active) {
            return null;
        }
        String canonical = canonical(entry);
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(keyMaterial, ALGO));
            byte[] digest = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException("Audit integrity hashing failed", e);
        }
    }

    public boolean verify(SystemAuditEntry entry, String storedHash) {
        if (!active || storedHash == null) {
            return true; // nothing to verify
        }
        return storedHash.equals(hash(entry));
    }

    private String canonical(SystemAuditEntry e) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("v1|");
        append(sb, e.id());
        append(sb, e.tenantId());
        append(sb, e.organizationId());
        append(sb, e.actorUserId());
        append(sb, e.createdAt());
        append(sb, e.action());
        append(sb, e.targetType());
        append(sb, e.targetId());
        append(sb, e.payloadSummary());
        append(sb, e.requestId());
        append(sb, e.clientApplicationId());
        append(sb, e.remoteIp());
        append(sb, e.httpMethod());
        append(sb, e.httpPath());
        return sb.toString();
    }

    private void append(StringBuilder sb, Object value) {
        sb.append(value == null ? "" : value.toString().replace("|", "\\|"));
        sb.append('|');
    }
}
