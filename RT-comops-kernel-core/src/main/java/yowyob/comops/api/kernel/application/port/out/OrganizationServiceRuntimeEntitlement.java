package yowyob.comops.api.kernel.application.port.out;

public record OrganizationServiceRuntimeEntitlement(
        String serviceCode,
        boolean effective,
        Long requestQuotaLimit,
        Long requestQuotaWindowSeconds) {
}
