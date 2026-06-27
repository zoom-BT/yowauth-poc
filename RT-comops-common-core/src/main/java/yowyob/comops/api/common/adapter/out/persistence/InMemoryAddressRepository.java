package yowyob.comops.api.common.adapter.out.persistence;

import yowyob.comops.api.common.application.port.out.AddressRepository;
import yowyob.comops.api.common.domain.model.Address;
import yowyob.comops.api.common.domain.model.AddressableType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryAddressRepository implements AddressRepository {

    private final Map<UUID, Address> addresses = new ConcurrentHashMap<>();

    @Override
    public Mono<Address> save(Address address) {
        return Mono.fromSupplier(() -> {
            addresses.put(address.id(), address);
            return address;
        });
    }

    @Override
    public Flux<Address> findByAddressable(UUID tenantId, AddressableType addressableType, UUID addressableId) {
        return Flux.fromStream(addresses.values().stream()
                .filter(address -> address.tenantId().equals(tenantId))
                .filter(address -> address.addressableType() == addressableType)
                .filter(address -> address.addressableId().equals(addressableId))
                .filter(address -> address.deletedAt() == null));
    }

    @Override
    public Mono<Void> deleteById(UUID tenantId, UUID addressId) {
        return Mono.fromRunnable(() -> addresses.computeIfPresent(addressId,
                (id, address) -> address.tenantId().equals(tenantId) ? null : address));
    }
}
