package yowyob.comops.api.common.application.port.in;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface DeleteContactUseCase {

    Mono<Void> deleteContact(UUID tenantId, UUID contactId);
}
