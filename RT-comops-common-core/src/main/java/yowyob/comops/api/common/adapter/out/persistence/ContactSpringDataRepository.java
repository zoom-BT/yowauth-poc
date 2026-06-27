package yowyob.comops.api.common.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ContactSpringDataRepository extends ReactiveCrudRepository<ContactEntity, UUID> {

    Flux<ContactEntity> findAllByTenantIdAndContactableTypeAndContactableIdAndDeletedAtIsNull(
            UUID tenantId, String contactableType, UUID contactableId);

    Mono<Void> deleteByIdAndTenantId(UUID id, UUID tenantId);
}
