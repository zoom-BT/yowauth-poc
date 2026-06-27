package yowyob.comops.api.actor.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;

public record BusinessActorRequest(
        String code,
        Boolean isIndividual,
        Boolean isAvailable,
        Boolean isVerified,
        Boolean isActive,
        String type,
        String role,
        Set<String> qualifications,
        Set<String> paymentMethods,
        Set<UUID> addresses,
        String biography,
        @NotBlank String name,
        String businessId,
        String niu,
        String tradeRegistryNumber,
        String website,
        String contactPhone,
        String privateAddress,
        String businessAddress,
        String businessProfile) {

    public boolean resolvedIndividual() {
        return Boolean.TRUE.equals(isIndividual);
    }

    public boolean resolvedAvailable() {
        return isAvailable == null || isAvailable;
    }

    public boolean resolvedVerified() {
        return Boolean.TRUE.equals(isVerified);
    }

    public boolean resolvedActive() {
        return isActive == null || isActive;
    }

    public String resolvedBiography() {
        if (biography != null && !biography.isBlank()) {
            return biography;
        }
        return businessProfile;
    }
}
