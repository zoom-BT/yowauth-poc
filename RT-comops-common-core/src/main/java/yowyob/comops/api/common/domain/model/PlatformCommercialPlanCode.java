package yowyob.comops.api.common.domain.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public enum PlatformCommercialPlanCode {
    STARTER("Starter", "Commercial identity and third-party foundation.",
            Set.of(), Set.of(PlatformServiceCode.COMMERCIAL.code()),
            Set.of("INVENTORY_OPERATIONS_ADDON", "HRM_PAYROLL_ADDON", "BLOCKCHAIN_AUDIT_ADDON", "NOTIFICATION_ADDON"),
            Map.of(PlatformServiceCode.COMMERCIAL.code(), new ServiceQuota(10_000L, 3_600L))),
    COMMERCE("Commerce", "Commercial pack with catalog, sales and billing.",
            Set.of(PlatformServicePackCode.COMMERCIAL_PACK.code()), Set.of(),
            Set.of("INVENTORY_OPERATIONS_ADDON", "POINT_OF_SALE_ADDON", "TREASURY_SETTLEMENTS_ADDON",
                    "HRM_PAYROLL_ADDON", "BLOCKCHAIN_AUDIT_ADDON", "NOTIFICATION_ADDON"),
            Map.of(
                    PlatformServiceCode.COMMERCIAL.code(), new ServiceQuota(40_000L, 3_600L),
                    PlatformServiceCode.PRODUCT.code(), new ServiceQuota(40_000L, 3_600L),
                    PlatformServiceCode.SALES.code(), new ServiceQuota(50_000L, 3_600L),
                    PlatformServiceCode.BILLING.code(), new ServiceQuota(50_000L, 3_600L))),
    FINANCE("Finance", "Finance pack for accounting, banking, treasury and cashier.",
            Set.of(PlatformServicePackCode.FINANCE_PACK.code()), Set.of(),
            Set.of("HRM_PAYROLL_ADDON", "BLOCKCHAIN_AUDIT_ADDON", "NOTIFICATION_ADDON"),
            Map.of(
                    PlatformServiceCode.ACCOUNTING.code(), new ServiceQuota(45_000L, 3_600L),
                    PlatformServiceCode.BANKING.code(), new ServiceQuota(45_000L, 3_600L),
                    PlatformServiceCode.TREASURY.code(), new ServiceQuota(45_000L, 3_600L),
                    PlatformServiceCode.CASHIER.code(), new ServiceQuota(60_000L, 3_600L))),
    OPERATIONS("Operations", "Operations pack for stock, resources, HRM and audit ledger.",
            Set.of(PlatformServicePackCode.OPERATIONS_PACK.code()), Set.of(),
            Set.of(),
            Map.of(
                    PlatformServiceCode.PRODUCT.code(), new ServiceQuota(40_000L, 3_600L),
                    PlatformServiceCode.INVENTORY.code(), new ServiceQuota(60_000L, 3_600L),
                    PlatformServiceCode.RESOURCE.code(), new ServiceQuota(40_000L, 3_600L),
                    PlatformServiceCode.HRM.code(), new ServiceQuota(35_000L, 3_600L),
                    PlatformServiceCode.BLOCKCHAIN.code(), new ServiceQuota(35_000L, 3_600L))),
    ENTERPRISE("Enterprise", "All platform modules with high operational quotas.",
            Set.of(PlatformServicePackCode.COMMERCIAL_PACK.code(), PlatformServicePackCode.FINANCE_PACK.code(),
                    PlatformServicePackCode.OPERATIONS_PACK.code()),
            Set.of(PlatformServiceCode.NOTIFICATION.code()), Set.of(), Map.ofEntries(
                    Map.entry(PlatformServiceCode.COMMERCIAL.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.PRODUCT.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.INVENTORY.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.SALES.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.BILLING.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.ACCOUNTING.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.BANKING.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.TREASURY.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.CASHIER.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.RESOURCE.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.HRM.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.BLOCKCHAIN.code(), new ServiceQuota(150_000L, 3_600L)),
                    Map.entry(PlatformServiceCode.NOTIFICATION.code(), new ServiceQuota(200_000L, 3_600L))));

    private final String displayName;
    private final String description;
    private final Set<String> packCodes;
    private final Set<String> serviceCodes;
    private final Set<String> compatibleAddOnCodes;
    private final Map<String, ServiceQuota> serviceQuotas;

    PlatformCommercialPlanCode(String displayName, String description, Set<String> packCodes, Set<String> serviceCodes,
            Set<String> compatibleAddOnCodes, Map<String, ServiceQuota> serviceQuotas) {
        this.displayName = displayName;
        this.description = description;
        this.packCodes = packCodes;
        this.serviceCodes = serviceCodes;
        this.compatibleAddOnCodes = compatibleAddOnCodes;
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

    public List<String> packCodes() {
        return PlatformServicePackCode.catalog().stream()
                .map(PlatformServicePackCode::code)
                .filter(packCodes::contains)
                .toList();
    }

    public List<String> serviceCodes() {
        LinkedHashSet<String> merged = new LinkedHashSet<>(serviceCodes);
        packCodes().stream()
                .map(PlatformServicePackCode::from)
                .flatMap(pack -> pack.serviceCodes().stream())
                .forEach(merged::add);
        return PlatformServiceCode.orderCodes(merged);
    }

    public List<String> compatibleAddOnCodes() {
        return PlatformCommercialAddOnCode.catalog().stream()
                .map(PlatformCommercialAddOnCode::code)
                .filter(compatibleAddOnCodes::contains)
                .toList();
    }

    public Map<String, ServiceQuota> serviceQuotas() {
        return serviceQuotas;
    }

    public static PlatformCommercialPlanCode from(String rawCode) {
        String normalized = normalize(rawCode);
        return Arrays.stream(values())
                .filter(plan -> plan.code().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown commercial plan: " + rawCode));
    }

    public static List<PlatformCommercialPlanCode> catalog() {
        return List.of(values());
    }

    static List<String> orderCodes(Set<String> codes) {
        return catalog().stream()
                .map(PlatformCommercialPlanCode::code)
                .filter(codes::contains)
                .toList();
    }

    public record ServiceQuota(long requestQuotaLimit, long requestQuotaWindowSeconds) {
    }

    private static String normalize(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("planCode is required");
        }
        return rawCode.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
