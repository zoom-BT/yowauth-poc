package yowyob.comops.api.actor.application.port.out;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface UserAccountDirectory {

    Mono<UserAccountLink> findByUserId(UUID tenantId, UUID userId);

    record UserAccountLink(UUID userId, UUID actorId, String username, String email) {
    }
}
