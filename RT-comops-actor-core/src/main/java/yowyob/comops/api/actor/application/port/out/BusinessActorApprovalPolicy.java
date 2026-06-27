package yowyob.comops.api.actor.application.port.out;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface BusinessActorApprovalPolicy {

    Mono<Boolean> requiresApproval(UUID tenantId);
}
