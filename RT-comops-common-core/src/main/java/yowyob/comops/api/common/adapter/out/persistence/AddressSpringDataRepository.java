package yowyob.comops.api.common.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AddressSpringDataRepository extends ReactiveCrudRepository<AddressEntity, UUID> {

    Flux<AddressEntity> findAllByTenantIdAndAddressableTypeAndAddressableIdAndDeletedAtIsNull(
            UUID tenantId, String addressableType, UUID addressableId);

    Mono<Void> deleteByIdAndTenantId(UUID id, UUID tenantId);
}
