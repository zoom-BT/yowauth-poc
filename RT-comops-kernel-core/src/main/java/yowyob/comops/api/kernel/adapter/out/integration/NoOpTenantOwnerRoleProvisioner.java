package yowyob.comops.api.kernel.adapter.out.integration;

import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import yowyob.comops.api.kernel.application.port.out.TenantOwnerRoleProvisioner;

/**
 * Fallback used when the roles module is not on the classpath (e.g. slice tests).
 * The real {@code RolesTenantOwnerRoleProvisioner} in roles-core is marked {@code @Primary}
 * and takes precedence in the full application.
 */
@Component
public class NoOpTenantOwnerRoleProvisioner implements TenantOwnerRoleProvisioner {

    @Override
    public Mono<Void> provisionOwner(UUID tenantId, UUID userId) {
        return Mono.empty();
    }
}
