package yowyob.comops.api.kernel.application.port.out;

import java.util.Set;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface ReactivePermissionCache {

    Mono<Set<String>> get(UUID tenantId, UUID userId);

    Mono<Void> put(UUID tenantId, UUID userId, Set<String> permissions);

    Mono<Void> evict(UUID tenantId, UUID userId);
}
