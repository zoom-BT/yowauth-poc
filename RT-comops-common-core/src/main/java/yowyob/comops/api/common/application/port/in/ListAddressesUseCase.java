package yowyob.comops.api.common.application.port.in;

import yowyob.comops.api.common.domain.model.Address;
import yowyob.comops.api.common.domain.model.AddressableType;
import java.util.UUID;
import reactor.core.publisher.Flux;

public interface ListAddressesUseCase {

    Flux<Address> listAddresses(UUID tenantId, AddressableType addressableType, UUID addressableId);
}
