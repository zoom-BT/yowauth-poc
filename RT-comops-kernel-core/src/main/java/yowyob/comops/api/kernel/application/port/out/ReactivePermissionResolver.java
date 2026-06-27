package yowyob.comops.api.kernel.application.port.out;

import java.util.Set;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface ReactivePermissionResolver {

    Mono<Set<String>> resolvePermissions(UUID tenantId, UUID userId);
}
