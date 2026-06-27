package yowyob.comops.api.common.application.port.in;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface DeleteAddressUseCase {

    Mono<Void> deleteAddress(UUID tenantId, UUID addressId);
}
