package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import java.util.List;

public final class PlatformServiceRouteResolver {

    private static final List<RouteServiceMapping> CLIENT_APPLICATION_MAPPINGS = List.of(
            new RouteServiceMapping("/api/organizations", PlatformServiceCode.ORGANIZATION.code()),
            new RouteServiceMapping("/api/agencies", PlatformServiceCode.ORGANIZATION.code()),
            new RouteServiceMapping("/api/pois", PlatformServiceCode.ORGANIZATION.code()),
            new RouteServiceMapping("/api/organizations/opening-hours", PlatformServiceCode.ORGANIZATION.code()),
            new RouteServiceMapping("/api/organizations/points-of-interest", PlatformServiceCode.ORGANIZATION.code()),
            new RouteServiceMapping("/api/general-options", PlatformServiceCode.SETTINGS.code()),
            new RouteServiceMapping("/api/generalOptions", PlatformServiceCode.SETTINGS.code()),
            new RouteServiceMapping("/api/settings/document-sequences", PlatformServiceCode.SETTINGS.code()),
            new RouteServiceMapping("/api/clients", PlatformServiceCode.COMMERCIAL.code()),
            new RouteServiceMapping("/api/customers", PlatformServiceCode.COMMERCIAL.code()),
            new RouteServiceMapping("/api/suppliers", PlatformServiceCode.COMMERCIAL.code()),
            new RouteServiceMapping("/api/prospects", PlatformServiceCode.COMMERCIAL.code()),
            new RouteServiceMapping("/api/sales-agents", PlatformServiceCode.COMMERCIAL.code()),
            new RouteServiceMapping("/api/third-parties", PlatformServiceCode.COMMERCIAL.code()),
            new RouteServiceMapping("/api/products", PlatformServiceCode.PRODUCT.code()),
            new RouteServiceMapping("/api/warehouses", PlatformServiceCode.INVENTORY.code()),
            new RouteServiceMapping("/api/inventory", PlatformServiceCode.INVENTORY.code()),
            new RouteServiceMapping("/api/inventories", PlatformServiceCode.INVENTORY.code()),
            new RouteServiceMapping("/api/sales", PlatformServiceCode.SALES.code()),
            new RouteServiceMapping("/api/accounting", PlatformServiceCode.ACCOUNTING.code()),
            new RouteServiceMapping("/api/accounting-service", PlatformServiceCode.ACCOUNTING.code()),
            new RouteServiceMapping("/api/comptable", PlatformServiceCode.ACCOUNTING.code()),
            new RouteServiceMapping("/api/v1/accounting", PlatformServiceCode.ACCOUNTING.code()),
            new RouteServiceMapping("/api/taxes", PlatformServiceCode.ACCOUNTING.code()),
            new RouteServiceMapping("/api/journals", PlatformServiceCode.ACCOUNTING.code()),
            new RouteServiceMapping("/api/bons-achat", PlatformServiceCode.BILLING.code()),
            new RouteServiceMapping("/api/bon-commande", PlatformServiceCode.BILLING.code()),
            new RouteServiceMapping("/api/bons-livraison", PlatformServiceCode.BILLING.code()),
            new RouteServiceMapping("/api/v1/facturation", PlatformServiceCode.BILLING.code()),
            new RouteServiceMapping("/api/facture-fournisseurs", PlatformServiceCode.BILLING.code()),
            new RouteServiceMapping("/api/factures-proforma", PlatformServiceCode.BILLING.code()),
            new RouteServiceMapping("/api/paiement", PlatformServiceCode.BILLING.code()),
            new RouteServiceMapping("/api/tableau-de-bord", PlatformServiceCode.BILLING.code()),
            new RouteServiceMapping("/api/treasury", PlatformServiceCode.TREASURY.code()),
            new RouteServiceMapping("/api/banking", PlatformServiceCode.BANKING.code()),
            new RouteServiceMapping("/api/banks", PlatformServiceCode.BANKING.code()),
            new RouteServiceMapping("/api/transaction-types", PlatformServiceCode.BANKING.code()),
            new RouteServiceMapping("/api/statement-lines", PlatformServiceCode.BANKING.code()),
            new RouteServiceMapping("/api/audit-logs", PlatformServiceCode.BANKING.code()),
            new RouteServiceMapping("/api/bank-accounts", PlatformServiceCode.BANKING.code()),
            new RouteServiceMapping("/api/bank-statements", PlatformServiceCode.BANKING.code()),
            new RouteServiceMapping("/api/checks", PlatformServiceCode.BANKING.code()),
            new RouteServiceMapping("/api/reconciliation", PlatformServiceCode.BANKING.code()),
            new RouteServiceMapping("/api/admin/accounts", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/cashier/accounts", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/accounts/transfer", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/accounts/withdraw", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/accounts/transfer-p2p", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/cashier/fund-requests", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/cashier/bills", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/bills", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/cash-registers", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/cashiers", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/cashier/sessions", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/dashboard/stats", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/dashboard/stat", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/admin/documents", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/config/denominations", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/cashier/movements", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/movements", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/transactions", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/audit", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/admin/reconciliations", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/cashier/reconciliations", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/reconciliations", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/reports/transactions", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/reports/register", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/reports/session", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/reports/audit", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/sessions", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/notify-unauthorized", PlatformServiceCode.CASHIER.code()),
            new RouteServiceMapping("/api/resources", PlatformServiceCode.RESOURCE.code()),
            new RouteServiceMapping("/api/employees", PlatformServiceCode.HRM.code()),
            new RouteServiceMapping("/api/v1/hrm", PlatformServiceCode.HRM.code()),
            new RouteServiceMapping("/api/v1/blockchain", PlatformServiceCode.BLOCKCHAIN.code()),
            new RouteServiceMapping("/api/notifications", PlatformServiceCode.NOTIFICATION.code()));

    private static final List<RouteServiceMapping> ORGANIZATION_ENTITLEMENT_MAPPINGS = CLIENT_APPLICATION_MAPPINGS.stream()
            .filter(mapping -> !PlatformServiceCode.ORGANIZATION.code().equals(mapping.serviceCode())
                    && !PlatformServiceCode.SETTINGS.code().equals(mapping.serviceCode()))
            .toList();

    String resolveClientApplicationServiceCode(String path) {
        return resolve(path, CLIENT_APPLICATION_MAPPINGS);
    }

    String resolveOrganizationEntitlementServiceCode(String path) {
        return resolve(path, ORGANIZATION_ENTITLEMENT_MAPPINGS);
    }

    private String resolve(String path, List<RouteServiceMapping> mappings) {
        return mappings.stream()
                .filter(mapping -> mapping.matches(path))
                .map(RouteServiceMapping::serviceCode)
                .findFirst()
                .orElse(null);
    }

    private record RouteServiceMapping(String prefix, String serviceCode) {
        private boolean matches(String path) {
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
    }
}
