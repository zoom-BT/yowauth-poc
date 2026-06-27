package yowyob.comops.api.kernel.config;

import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("businessAccessPolicy")
public class BusinessAccessPolicy {

    public boolean canManageIdentity(Authentication authentication) {
        if (!(authentication instanceof ApiKeyAuthenticationToken token) || !authentication.isAuthenticated()
                || token.tenantId() == null) {
            return false;
        }
        if (token.userId() == null) {
            return false;
        }
        return hasAnyPermission(authentication, Set.of("system:admin", "iam:admin", "tenant:admin"));
    }

    public boolean canManageClientApplications(Authentication authentication) {
        return hasUserContext(authentication)
                && hasAnyPermission(authentication, Set.of("system:admin", "iam:admin"));
    }

    public boolean canReadAdministration(Authentication authentication) {
        return hasUserContext(authentication) && hasAnyPermission(authentication,
                Set.of("administration:read", "administration:write", "administration:roles:read",
                        "administration:settings:read", "system:admin", "iam:admin", "tenant:admin"));
    }

    public boolean canWriteAdministration(Authentication authentication) {
        return hasUserContext(authentication) && hasAnyPermission(authentication,
                Set.of("administration:write", "administration:assignments:write", "system:admin", "iam:admin",
                        "tenant:admin"));
    }

    public boolean canManageAdministrativeRoles(Authentication authentication) {
        return hasUserContext(authentication) && hasAnyPermission(authentication,
                Set.of("administration:roles:read", "administration:roles:write", "administration:assignments:write",
                        "administration:write", "system:admin", "iam:admin", "tenant:admin"));
    }

    public boolean canCloneAdministrativeRoles(Authentication authentication) {
        return hasUserContext(authentication) && hasAnyPermission(authentication,
                Set.of("administration:roles:clone", "administration:roles:write", "administration:write",
                        "system:admin", "iam:admin", "tenant:admin"));
    }

    public boolean canManageAdministrativeSettings(Authentication authentication) {
        return hasUserContext(authentication) && hasAnyPermission(authentication,
                Set.of("administration:settings:read", "administration:settings:write", "administration:write",
                        "system:admin", "iam:admin", "tenant:admin"));
    }

    public boolean canReadAdministrativeAudit(Authentication authentication) {
        return hasUserContext(authentication) && hasAnyPermission(authentication,
                Set.of("administration:audit:read", "administration:read", "administration:write",
                        "system:admin", "iam:admin", "tenant:admin"));
    }

    public boolean canGovernBusinessActors(Authentication authentication) {
        return hasUserContext(authentication) && hasAnyPermission(authentication,
                Set.of("administration:govern:business-actors", "system:admin", "iam:admin", "tenant:admin"));
    }

    public boolean canGovernOrganizations(Authentication authentication) {
        return hasUserContext(authentication) && hasAnyPermission(authentication,
                Set.of("administration:govern:organizations", "system:admin", "iam:admin", "tenant:admin"));
    }

    public boolean canGovernAgencies(Authentication authentication) {
        return hasUserContext(authentication) && hasAnyPermission(authentication,
                Set.of("administration:govern:agencies", "system:admin", "iam:admin", "tenant:admin"));
    }

    public boolean canPostInvoice(Authentication authentication) {
        return hasUserContext(authentication) && hasAnyPermission(authentication,
                Set.of("accounting:post", "accounting:write"));
    }

    public boolean canReadAccounting(Authentication authentication) {
        return hasUserContext(authentication)
                && hasAnyPermission(authentication, Set.of("accounting:read", "accounting:write", "accounting:post"));
    }

    public boolean canRegisterInvoiceSettlement(Authentication authentication) {
        return hasUserContext(authentication)
                && hasAnyPermission(authentication, Set.of("treasury:settle-invoices", "treasury:manage"));
    }

    public boolean canReadTreasury(Authentication authentication) {
        return hasUserContext(authentication)
                && hasAnyPermission(authentication, Set.of("treasury:read", "treasury:manage"));
    }

    public boolean canReserveResource(Authentication authentication) {
        return hasUserContext(authentication)
                && hasAnyPermission(authentication, Set.of("resources:reserve", "resources:write"));
    }

    public boolean canUnassignResource(Authentication authentication) {
        return hasUserContext(authentication)
                && hasAnyPermission(authentication, Set.of("resources:unassign", "resources:write"));
    }

    public boolean canDisposeResource(Authentication authentication) {
        return hasUserContext(authentication)
                && hasAnyPermission(authentication, Set.of("resources:dispose", "resources:write"));
    }

    public boolean hasPermission(Authentication authentication, String permission) {
        if (!hasUserContext(authentication)) {
            return false;
        }
        if (!(authentication instanceof ApiKeyAuthenticationToken token)) {
            return false;
        }
        if (hasExactAuthority(authentication, permission)
                || hasExactAuthority(authentication, permission + "#SYSTEM")
                || hasExactAuthority(authentication, permission + "#TENANT")) {
            return true;
        }
        if (token.organizationId() != null && hasExactAuthority(authentication,
                permission + "#ORGANIZATION:" + token.organizationId())) {
            return true;
        }
        return token.agencyId() != null && hasExactAuthority(authentication,
                permission + "#AGENCY:" + token.agencyId());
    }

    public boolean hasUserContext(Authentication authentication) {
        return authentication instanceof ApiKeyAuthenticationToken token
                && authentication.isAuthenticated()
                && token.tenantId() != null
                && token.userId() != null;
    }

    /**
     * Autorise une ClientApplication (service-à-service) habilitée au service KYC à récupérer le
     * contenu binaire d'un fichier en quarantaine pour analyse (ex: VerifID). Pas besoin de contexte
     * utilisateur : c'est un appel serveur-à-serveur authentifié par clé d'API.
     */
    public boolean canFetchFileForAnalysis(Authentication authentication) {
        return authentication instanceof ApiKeyAuthenticationToken token
                && authentication.isAuthenticated()
                && token.tenantId() != null
                && token.allowedServiceCodes().contains("KYC");
    }

    /**
     * Autorise toute ClientApplication authentifiée (clé d'API valide) à introspecter SES PROPRES
     * droits (endpoint /api/client-applications/me). Sert aussi aux services externes pour vérifier,
     * sur un appel serveur-à-serveur, à quels services le client appelant a droit (sans abus).
     */
    public boolean isAuthenticatedClientApplication(Authentication authentication) {
        return authentication instanceof ApiKeyAuthenticationToken token
                && authentication.isAuthenticated()
                && token.clientApplicationId() != null;
    }

    private boolean hasAnyPermission(Authentication authentication, Set<String> authorities) {
        return authorities.stream().anyMatch(authority -> hasPermission(authentication, authority));
    }

    private boolean hasExactAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
