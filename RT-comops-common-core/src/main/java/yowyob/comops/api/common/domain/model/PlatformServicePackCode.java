package yowyob.comops.api.common.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum PlatformServicePackCode {
    COMMERCIAL_PACK("Commercial Pack", "Commercial onboarding, catalog, sales and billing front-office flows.",
            Set.of(
                    PlatformServiceCode.COMMERCIAL.code(),
                    PlatformServiceCode.PRODUCT.code(),
                    PlatformServiceCode.SALES.code(),
                    PlatformServiceCode.BILLING.code())),
    FINANCE_PACK("Finance Pack", "Accounting, banking, treasury, cashier and settlement-oriented flows.",
            Set.of(
                    PlatformServiceCode.ACCOUNTING.code(),
                    PlatformServiceCode.BANKING.code(),
                    PlatformServiceCode.TREASURY.code(),
                    PlatformServiceCode.CASHIER.code())),
    OPERATIONS_PACK("Operations Pack", "Inventory, resources, HRM and operational audit support.",
            Set.of(
                    PlatformServiceCode.PRODUCT.code(),
                    PlatformServiceCode.INVENTORY.code(),
                    PlatformServiceCode.RESOURCE.code(),
                    PlatformServiceCode.HRM.code(),
                    PlatformServiceCode.BLOCKCHAIN.code()));

    private final String displayName;
    private final String description;
    private final Set<String> serviceCodes;

    PlatformServicePackCode(String displayName, String description, Set<String> serviceCodes) {
        this.displayName = displayName;
        this.description = description;
        this.serviceCodes = serviceCodes;
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

    public static PlatformServicePackCode from(String rawCode) {
        String normalized = normalize(rawCode);
        return Arrays.stream(values())
                .filter(pack -> pack.code().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown platform service pack: " + rawCode));
    }

    public static List<PlatformServicePackCode> catalog() {
        return List.of(values());
    }

    public static List<String> packCodesForService(String serviceCode) {
        PlatformServiceCode normalizedService = PlatformServiceCode.from(serviceCode);
        return catalog().stream()
                .filter(pack -> pack.serviceCodes.contains(normalizedService.code()))
                .map(PlatformServicePackCode::code)
                .toList();
    }

    private static String normalize(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("servicePackCode is required");
        }
        return rawCode.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
