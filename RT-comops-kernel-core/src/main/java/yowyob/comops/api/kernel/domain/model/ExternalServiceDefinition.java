package yowyob.comops.api.kernel.domain.model;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;

/**
 * Service EXTERNE enregistré dynamiquement (hors enum {@link PlatformServiceCode}). Permet d'ajouter
 * de nouveaux services à la plateforme SANS toucher au code : une fois enregistré, son code peut
 * figurer dans les {@code allowedServices} d'une ClientApplication et être autorisé via
 * {@code /api/client-applications/me/authorize}.
 */
public record ExternalServiceDefinition(String code, String displayName, String description, boolean active) {

    public ExternalServiceDefinition {
        code = PlatformServiceCode.normalizeCode(code);
        if (displayName == null || displayName.isBlank()) {
            displayName = code;
        }
    }

    public static ExternalServiceDefinition register(String code, String displayName, String description) {
        return new ExternalServiceDefinition(code, displayName, description, true);
    }
}
