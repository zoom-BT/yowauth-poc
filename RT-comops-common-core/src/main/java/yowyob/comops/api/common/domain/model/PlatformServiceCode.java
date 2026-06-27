package yowyob.comops.api.common.domain.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum PlatformServiceCode {
    ORGANIZATION("Organization", "Core organization, agencies and base governance structures.", true, false,
            Set.of("ORGANIZATION", "ORGANIZATIONS", "ORG"),
            Set.of(), Set.of()),
    SETTINGS("Settings", "Core business settings and document sequences.", true, false,
            Set.of("SETTINGS", "SETTING", "CONFIG"),
            Set.of(), Set.of()),
    COMMERCIAL("Commercial", "Clients, suppliers, prospects, sales agents and third parties.", false, true,
            Set.of("COMMERCIAL", "THIRD_PARTY", "THIRD_PARTIES", "TIERS", "TIER"),
            Set.of(), Set.of()),
    PRODUCT("Product", "Product catalog management.", false, true,
            Set.of("PRODUCT", "PRODUCTS", "CATALOG"),
            Set.of(), Set.of("COMMERCIAL")),
    INVENTORY("Inventory", "Stock movements, sessions, transfers, warehouses and transformations.", false, true,
            Set.of("INVENTORY", "STOCK", "INVENTAIRE"),
            Set.of("PRODUCT"), Set.of("RESOURCE")),
    SALES("Sales", "Sales orders and confirmations.", false, true,
            Set.of("SALES", "SALE", "VENTE", "VENTES"),
            Set.of("COMMERCIAL", "PRODUCT"), Set.of()),
    BILLING("Billing", "Commercial billing documents and payment workflows.", false, true,
            Set.of("BILLING", "FACTURATION"),
            Set.of("COMMERCIAL"), Set.of("ACCOUNTING", "CASHIER", "TREASURY")),
    ACCOUNTING("Accounting", "Accounting, taxes, journals, reports and bookkeeping.", false, true,
            Set.of("ACCOUNTING", "COMPTABILITE", "COMPTA"),
            Set.of(), Set.of("COMMERCIAL")),
    BANKING("Banking", "Bank accounts, statements, banking transactions, checks and reconciliation workflows.", false, true,
            Set.of("BANKING", "BANQUE", "BANK"),
            Set.of(), Set.of("ACCOUNTING")),
    TREASURY("Treasury", "Treasury settlements, treasury bank accounts and settlement reconciliations.", false, true,
            Set.of("TREASURY", "TRESORERIE"),
            Set.of("ACCOUNTING", "BANKING"), Set.of("BILLING")),
    CASHIER("Cashier", "Cash registers, cashier sessions, cash movements and cashier operations.", false, true,
            Set.of("CASHIER", "CAISSE", "CASH"),
            Set.of("ACCOUNTING"), Set.of("BILLING")),
    RESOURCE("Resource", "Material resource lifecycle, reservations and assignments.", false, true,
            Set.of("RESOURCE", "RESOURCES", "RESSOURCE", "RESSOURCES"),
            Set.of(), Set.of("INVENTORY")),
    HRM("HRM", "Human resources management, payroll, leave, training and compliance.", false, true,
            Set.of("HRM", "RH", "HUMAN_RESOURCES"),
            Set.of(), Set.of()),
    PAYROLL("Payroll", "Payroll runs, payslips, final settlements, garnishments and payroll declarations.", false, true,
            Set.of("PAYROLL", "PAIE", "PAYSLIP", "PAYSLIPS"),
            Set.of("HRM"), Set.of("ACCOUNTING")),
    BLOCKCHAIN("Blockchain", "Cryptographic ledger, signed transactions, proof-of-work blocks and document anchoring.",
            false, true, Set.of("BLOCKCHAIN", "CHAIN", "LEDGER", "AUDIT_LEDGER"),
            Set.of(), Set.of("ACCOUNTING", "BILLING")),
    NOTIFICATION("Notification", "Email, SMS, WhatsApp, push, websocket notifications, templates and delivery history.",
            false, true, Set.of("NOTIFICATION", "NOTIFICATIONS", "NOTIFY", "MESSAGING"),
            Set.of(), Set.of("ORGANIZATION", "SETTINGS")),
    YOWPAINTER("YowPainter", "Artist profiles, artworks, gallery events, social features and artwork commerce.",
            false, true, Set.of("YOWPAINTER", "PAINTER", "ART", "GALLERY"),
            Set.of("ORGANIZATION", "PRODUCT"), Set.of("SALES", "ACCOUNTING", "BILLING")),
    KYC("KYC", "Identity document verification (OCR + AI) for KYC documents, via the VerifID service.",
            false, true, Set.of("KYC", "VERIFID", "IDENTITY", "ID_VERIFICATION"),
            Set.of(), Set.of());

    private final String displayName;
    private final String description;
    private final boolean mandatory;
    private final boolean subscribable;
    private final Set<String> aliases;
    private final Set<String> requiredDependencies;
    private final Set<String> recommendedDependencies;

    PlatformServiceCode(String displayName, String description, boolean mandatory, boolean subscribable,
            Set<String> aliases, Set<String> requiredDependencies, Set<String> recommendedDependencies) {
        this.displayName = displayName;
        this.description = description;
        this.mandatory = mandatory;
        this.subscribable = subscribable;
        this.aliases = aliases;
        this.requiredDependencies = requiredDependencies;
        this.recommendedDependencies = recommendedDependencies;
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

    public boolean mandatory() {
        return mandatory;
    }

    public boolean subscribable() {
        return subscribable;
    }

    public List<String> requiredDependencyCodes() {
        return orderCodes(requiredDependencies);
    }

    public List<String> recommendedDependencyCodes() {
        return orderCodes(recommendedDependencies);
    }

    public static PlatformServiceCode from(String rawCode) {
        String normalized = normalize(rawCode);
        return Arrays.stream(values())
                .filter(service -> service.code().equals(normalized) || service.aliases.contains(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown platform service: " + rawCode));
    }

    public static List<PlatformServiceCode> catalog() {
        return List.of(values());
    }

    public static List<String> mandatoryCodes() {
        return catalog().stream()
                .filter(PlatformServiceCode::mandatory)
                .map(PlatformServiceCode::code)
                .toList();
    }

    public static List<String> subscribableCodes() {
        return catalog().stream()
                .filter(PlatformServiceCode::subscribable)
                .map(PlatformServiceCode::code)
                .toList();
    }

    public static List<String> orderCodes(Set<String> codes) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        // 1) services natifs (enum) dans l'ordre du catalogue
        catalog().stream()
                .map(PlatformServiceCode::code)
                .filter(codes::contains)
                .forEach(ordered::add);
        // 2) services EXTERNES enregistrés (hors enum) : conservés, triés, ajoutés à la fin
        codes.stream()
                .filter(code -> code != null && !ordered.contains(code))
                .sorted()
                .forEach(ordered::add);
        return List.copyOf(ordered);
    }

    /**
     * Normalise le FORMAT d'un code service (majuscules, séparateurs) SANS exiger qu'il soit dans
     * l'enum. Utilisé pour accepter les services EXTERNES enregistrés dynamiquement (registre DB).
     */
    public static String normalizeCode(String rawCode) {
        return normalize(rawCode);
    }

    /**
     * Renvoie le code canonique : celui de l'enum si connu (gère les alias), sinon le code
     * simplement normalisé (cas d'un service externe enregistré hors enum). Ne lève jamais
     * pour un code externe inconnu de l'enum.
     */
    public static String resolveCanonical(String rawCode) {
        String normalized = normalize(rawCode);
        return Arrays.stream(values())
                .filter(service -> service.code().equals(normalized) || service.aliases.contains(normalized))
                .map(PlatformServiceCode::code)
                .findFirst()
                .orElse(normalized);
    }

    private static String normalize(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("serviceCode is required");
        }
        return rawCode.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
