package yowyob.comops.api.common.adapter.out.persistence;

import yowyob.comops.api.common.application.port.out.AddressRepository;
import yowyob.comops.api.common.domain.model.Address;
import yowyob.comops.api.common.domain.model.AddressType;
import yowyob.comops.api.common.domain.model.AddressableType;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class AddressR2dbcRepositoryAdapter implements AddressRepository {

    private final AddressSpringDataRepository repository;

    public AddressR2dbcRepositoryAdapter(AddressSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Address> save(Address address) {
        return repository.save(toEntity(address)).map(this::toDomain);
    }

    @Override
    public Flux<Address> findByAddressable(UUID tenantId, AddressableType addressableType, UUID addressableId) {
        return repository.findAllByTenantIdAndAddressableTypeAndAddressableIdAndDeletedAtIsNull(
                tenantId, addressableType.name(), addressableId).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(UUID tenantId, UUID addressId) {
        return repository.deleteByIdAndTenantId(addressId, tenantId);
    }

    private AddressEntity toEntity(Address address) {
        return new AddressEntity(address.id(), address.tenantId(), address.createdAt(), address.updatedAt(),
                address.deletedAt(), address.addressableType().name(), address.addressableId(),
                address.type().name(), address.addressLine1(), address.addressLine2(), address.city(), address.state(),
                address.locality(), address.countryId(), address.zipCode(), address.postalCode(), address.poBox(),
                address.isDefault(), address.neighborhood(), address.informalDescription(), address.latitude(),
                address.longitude());
    }

    private Address toDomain(AddressEntity entity) {
        return new Address(entity.id(), entity.tenantId(), AddressableType.valueOf(entity.addressableType()),
                entity.addressableId(), AddressType.valueOf(entity.type()), entity.addressLine1(), entity.addressLine2(),
                entity.city(), entity.state(), entity.locality(), entity.countryId(), entity.zipCode(),
                entity.postalCode(), entity.poBox(), entity.isDefault(), entity.neighborhood(),
                entity.informalDescription(), entity.latitude(), entity.longitude(), entity.createdAt(),
                entity.updatedAt(), entity.deletedAt());
    }
}
