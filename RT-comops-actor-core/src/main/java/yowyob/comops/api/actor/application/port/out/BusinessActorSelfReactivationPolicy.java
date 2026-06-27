package yowyob.comops.api.actor.application.port.out;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface BusinessActorSelfReactivationPolicy {

    Mono<Boolean> isSelfReactivationAllowed(UUID tenantId);
}
