package yowyob.comops.api.kernel.application.port.out;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Provisions the default owner role and assignment for the first user of a freshly
 * created tenant, so a self-service sign-up that creates its own organisation is granted
 * the permissions required to operate the tenant (create the organisation, etc.).
 *
 * <p>The real implementation lives in the roles module; auth-core depends only on this
 * kernel port to avoid a module dependency cycle.</p>
 */
public interface TenantOwnerRoleProvisioner {

    Mono<Void> provisionOwner(UUID tenantId, UUID userId);
}
