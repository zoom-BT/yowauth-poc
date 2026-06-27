package yowyob.comops.api.common.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public enum PlatformCommercialAddOnCode {
    BLOCKCHAIN_AUDIT_ADDON("Blockchain Audit Add-on", "Cryptographic ledger and document anchoring.",
            Set.of(PlatformServiceCode.BLOCKCHAIN.code()),
            Set.of("STARTER", "COMMERCE", "FINANCE", "OPERATIONS", "ENTERPRISE"),
            Map.of(PlatformServiceCode.BLOCKCHAIN.code(), new ServiceQuota(30_000L, 3_600L))),
    HRM_PAYROLL_ADDON("HRM Payroll Add-on", "Human resources, payroll and employee operations.",
            Set.of(PlatformServiceCode.HRM.code()),
            Set.of("STARTER", "COMMERCE", "FINANCE", "OPERATIONS", "ENTERPRISE"),
            Map.of(PlatformServiceCode.HRM.code(), new ServiceQuota(20_000L, 3_600L))),
    NOTIFICATION_ADDON("Notification Add-on", "Email, SMS, WhatsApp, push and websocket notification flows.",
            Set.of(PlatformServiceCode.NOTIFICATION.code()),
            Set.of("STARTER", "COMMERCE", "FINANCE", "OPERATIONS", "ENTERPRISE"),
            Map.of(PlatformServiceCode.NOTIFICATION.code(), new ServiceQuota(80_000L, 3_600L))),
    INVENTORY_OPERATIONS_ADDON("Inventory Operations Add-on", "Product catalog, stock, inventory and resources.",
            Set.of(PlatformServiceCode.PRODUCT.code(), PlatformServiceCode.INVENTORY.code(),
                    PlatformServiceCode.RESOURCE.code()),
            Set.of("STARTER", "COMMERCE", "FINANCE", "ENTERPRISE"),
            Map.of(
                    PlatformServiceCode.PRODUCT.code(), new ServiceQuota(30_000L, 3_600L),
                    PlatformServiceCode.INVENTORY.code(), new ServiceQuota(40_000L, 3_600L),
                    PlatformServiceCode.RESOURCE.code(), new ServiceQuota(25_000L, 3_600L))),
    POINT_OF_SALE_ADDON("Point of Sale Add-on", "Cashier operations with required accounting foundation.",
            Set.of(PlatformServiceCode.ACCOUNTING.code(), PlatformServiceCode.CASHIER.code()),
            Set.of("COMMERCE", "FINANCE", "ENTERPRISE"),
            Map.of(
                    PlatformServiceCode.ACCOUNTING.code(), new ServiceQuota(25_000L, 3_600L),
                    PlatformServiceCode.CASHIER.code(), new ServiceQuota(60_000L, 3_600L))),
    TREASURY_SETTLEMENTS_ADDON("Treasury Settlements Add-on", "Banking, accounting and treasury settlement flows.",
            Set.of(PlatformServiceCode.ACCOUNTING.code(), PlatformServiceCode.BANKING.code(),
                    PlatformServiceCode.TREASURY.code()),
            Set.of("COMMERCE", "ENTERPRISE"),
            Map.of(
                    PlatformServiceCode.ACCOUNTING.code(), new ServiceQuota(35_000L, 3_600L),
                    PlatformServiceCode.BANKING.code(), new ServiceQuota(35_000L, 3_600L),
                    PlatformServiceCode.TREASURY.code(), new ServiceQuota(35_000L, 3_600L)));

    private final String displayName;
    private final String description;
    private final Set<String> serviceCodes;
    private final Set<String> compatiblePlanCodes;
    private final Map<String, ServiceQuota> serviceQuotas;

    PlatformCommercialAddOnCode(String displayName, String description, Set<String> serviceCodes,
            Set<String> compatiblePlanCodes, Map<String, ServiceQuota> serviceQuotas) {
        this.displayName = displayName;
        this.description = description;
        this.serviceCodes = serviceCodes;
        this.compatiblePlanCodes = compatiblePlanCodes;
        this.serviceQuotas = serviceQuotas;
    }

    public String code() {
        return name();
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public List<String> serviceCodes() {
        return PlatformServiceCode.orderCodes(serviceCodes);
    }

    public List<String> compatiblePlanCodes() {
        return PlatformCommercialPlanCode.orderCodes(compatiblePlanCodes);
    }

    public Map<String, ServiceQuota> serviceQuotas() {
        return serviceQuotas;
    }

    public static PlatformCommercialAddOnCode from(String rawCode) {
        String normalized = normalize(rawCode);
        return Arrays.stream(values())
                .filter(addOn -> addOn.code().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown commercial add-on: " + rawCode));
    }

    public static List<PlatformCommercialAddOnCode> catalog() {
        return List.of(values());
    }

    public record ServiceQuota(long requestQuotaLimit, long requestQuotaWindowSeconds) {
    }

    private static String normalize(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("addOnCode is required");
        }
        return rawCode.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
