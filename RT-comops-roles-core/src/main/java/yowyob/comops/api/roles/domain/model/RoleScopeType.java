package yowyob.comops.api.roles.domain.model;

import java.util.Locale;

public enum RoleScopeType {
    SYSTEM,
    TENANT,
    ORGANIZATION,
    AGENCY;

    public static RoleScopeType from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return TENANT;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if ("GLOBAL".equals(normalized)) {
            return TENANT;
        }
        return RoleScopeType.valueOf(normalized);
    }

    public String legacyPrefix() {
        return switch (this) {
            case SYSTEM -> "SYSTEM";
            case TENANT -> "GLOBAL";
            case ORGANIZATION -> "ORGANIZATION";
            case AGENCY -> "AGENCY";
        };
    }
}
