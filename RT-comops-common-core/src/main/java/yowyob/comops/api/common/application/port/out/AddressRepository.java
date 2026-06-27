package yowyob.comops.api.common.application.port.out;

import yowyob.comops.api.common.domain.model.Address;
import yowyob.comops.api.common.domain.model.AddressableType;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AddressRepository {

    Mono<Address> save(Address address);

    Flux<Address> findByAddressable(UUID tenantId, AddressableType addressableType, UUID addressableId);

    Mono<Void> deleteById(UUID tenantId, UUID addressId);
}
