package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Vue d'introspection renvoyée par {@code GET /api/client-applications/me} : ce que la
 * ClientApplication appelante a le droit de faire. Permet à un client (ou à un service externe
 * qui valide l'appelant) de connaître ses services autorisés AVANT d'agir, plutôt que de
 * découvrir l'interdiction via un 403. Le kernel reste seul juge (les filtres bloquent de toute
 * façon), ceci est l'API de découverte.
 */
public record MyClientApplicationResponse(
        UUID clientApplicationId,
        String clientId,
        UUID tenantId,
        UUID organizationId,
        UUID agencyId,
        List<String> allowedServices,
        List<ServiceEntitlement> services) {

    public record ServiceEntitlement(String code, String displayName, String description) {
    }

    public static MyClientApplicationResponse from(
            UUID clientApplicationId, String clientId, UUID tenantId, UUID organizationId, UUID agencyId,
            Set<String> allowedServiceCodes) {
        List<String> ordered = PlatformServiceCode.orderCodes(allowedServiceCodes);
        List<ServiceEntitlement> details = ordered.stream()
                .map(code -> {
                    try {
                        PlatformServiceCode svc = PlatformServiceCode.from(code);
                        return new ServiceEntitlement(svc.code(), svc.displayName(), svc.description());
                    } catch (RuntimeException unknown) {
                        return new ServiceEntitlement(code, code, null);
                    }
                })
                .toList();
        return new MyClientApplicationResponse(clientApplicationId, clientId, tenantId, organizationId, agencyId,
                ordered, details);
    }
}
