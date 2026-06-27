package yowyob.comops.api.kernel.application.port.out;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface OrganizationServiceRuntimeEntitlementDirectory {

    Mono<OrganizationServiceRuntimeEntitlement> resolveRuntimeEntitlement(UUID tenantId, UUID organizationId,
            String serviceCode);
}
