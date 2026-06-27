package yowyob.comops.api.pocapp.config;

import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.application.port.out.SignUpContextDirectory;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccess;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccessDirectory;
import yowyob.comops.api.kernel.application.port.out.OrganizationServiceRuntimeEntitlement;
import yowyob.comops.api.kernel.application.port.out.OrganizationServiceRuntimeEntitlementDirectory;

/**
 * Adaptateurs de sortie in-memory remplaçant, pour le slice POC, les adaptateurs
 * réels (bootstrap -> organization-core). La logique d'auth reste réelle ; seules
 * les données d'organisation sont simulées et seedées.
 */
@Configuration
public class InMemoryOrganizationAdaptersConfig {

    public static final UUID DEMO_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID DEMO_ORG_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    public static final String DEMO_ORG_CODE = "YOWAUTH-DEMO";

    @Bean
    OrganizationServiceRuntimeEntitlementDirectory inMemoryEntitlementDirectory() {
        return (tenantId, organizationId, serviceCode) ->
                Mono.just(new OrganizationServiceRuntimeEntitlement(serviceCode, true, null, null));
    }

    @Bean
    UserOrganizationAccessDirectory inMemoryUserOrganizationAccessDirectory() {
        return (tenantId, userId) -> Flux.just(new UserOrganizationAccess(
                DEMO_ORG_ID, DEMO_ORG_CODE, "YowAuth Demo", "YowAuth Demo Organization",
                List.of("SALES", "IAM")));
    }

    @Bean
    SignUpContextDirectory inMemorySignUpContextDirectory() {
        return organizationCode -> {
            if (organizationCode == null || !DEMO_ORG_CODE.equalsIgnoreCase(organizationCode)) {
                return Flux.empty();
            }
            return Flux.just(new SignUpContextDirectory.SignUpContext(
                    DEMO_ORG_ID.toString(), DEMO_TENANT_ID, DEMO_ORG_ID, DEMO_ORG_CODE,
                    "YowAuth Demo Organization", "COMPANY"));
        };
    }
}
