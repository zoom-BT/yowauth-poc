package yowyob.comops.api.common.application.port.in;

import yowyob.comops.api.common.domain.model.Address;
import reactor.core.publisher.Mono;

public interface CreateAddressUseCase {

    Mono<Address> createAddress(CreateAddressCommand command);
}
