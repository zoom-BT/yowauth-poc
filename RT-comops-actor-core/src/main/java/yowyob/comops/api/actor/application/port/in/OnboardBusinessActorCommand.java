package yowyob.comops.api.actor.application.port.in;

import java.util.Set;
import java.util.UUID;

public record OnboardBusinessActorCommand(
        UUID tenantId,
        UUID userId,
        String code,
        boolean isIndividual,
        boolean isAvailable,
        boolean isVerified,
        boolean isActive,
        String type,
        String role,
        Set<String> qualifications,
        Set<String> paymentMethods,
        Set<UUID> addresses,
        String biography,
        String name,
        String businessId,
        String niu,
        String tradeRegistryNumber,
        String website,
        String contactPhone,
        String privateAddress,
        String businessAddress,
        String businessProfile) {

    public OnboardBusinessActorCommand(
            UUID tenantId,
            UUID userId,
            String name,
            String businessId,
            String niu,
            String tradeRegistryNumber,
            String website,
            String contactPhone,
            String privateAddress,
            String businessAddress,
            String businessProfile) {
        this(
                tenantId,
                userId,
                null,
                false,
                true,
                false,
                true,
                null,
                null,
                Set.of(),
                Set.of(),
                Set.of(),
                businessProfile,
                name,
                businessId,
                niu,
                tradeRegistryNumber,
                website,
                contactPhone,
                privateAddress,
                businessAddress,
                businessProfile);
    }
}
