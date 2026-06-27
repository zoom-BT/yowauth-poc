package yowyob.comops.api.actor.adapter.out.persistence;

import yowyob.comops.api.actor.application.port.out.BusinessActorProfileRepository;
import yowyob.comops.api.actor.domain.model.BusinessActorProfile;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class BusinessActorProfileR2dbcRepositoryAdapter implements BusinessActorProfileRepository {

    private final BusinessActorProfileSpringDataRepository repository;

    public BusinessActorProfileR2dbcRepositoryAdapter(BusinessActorProfileSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<BusinessActorProfile> findById(UUID tenantId, UUID businessActorProfileId) {
        return repository.findByIdAndTenantId(businessActorProfileId, tenantId).map(this::toDomain);
    }

    @Override
    public Mono<BusinessActorProfile> findByActorId(UUID tenantId, UUID actorId) {
        return repository.findByTenantIdAndActorId(tenantId, actorId).map(this::toDomain);
    }

    @Override
    public Flux<BusinessActorProfile> findByTenantId(UUID tenantId) {
        return repository.findAllByTenantId(tenantId).map(this::toDomain);
    }

    @Override
    public Mono<BusinessActorProfile> save(BusinessActorProfile profile) {
        return repository.save(toEntity(profile)).map(this::toDomain);
    }

    private BusinessActorProfileEntity toEntity(BusinessActorProfile profile) {
        return new BusinessActorProfileEntity(profile.id(), profile.tenantId(), profile.createdAt(),
                profile.updatedAt(), profile.actorId(), profile.governanceStatus().name(),
                profile.governedByUserId(), profile.governedAt(), profile.governanceReason(), profile.code(),
                profile.isIndividual(), profile.isAvailable(), profile.isVerified(), profile.isActive(),
                profile.type(), profile.role(), profile.qualifications(), profile.paymentMethods(),
                profile.addresses(), profile.biography(), profile.deletedAt(), profile.name(), profile.businessId(),
                profile.niu(), profile.tradeRegistryNumber(), profile.website(), profile.contactPhone(),
                profile.privateAddress(), profile.businessAddress(), profile.businessProfile());
    }

    private BusinessActorProfile toDomain(BusinessActorProfileEntity entity) {
        return BusinessActorProfile.rehydrate(entity.id(), entity.tenantId(), entity.createdAt(), entity.updatedAt(),
                entity.actorId(), entity.governanceStatus(), entity.governedByUserId(), entity.governedAt(),
                entity.governanceReason(), entity.code(), entity.isIndividual(), entity.isAvailable(),
                entity.isVerified(), entity.isActive(), entity.type(), entity.role(), entity.qualifications(),
                entity.paymentMethods(), entity.addresses(), entity.biography(), entity.deletedAt(), entity.name(),
                entity.businessId(), entity.niu(), entity.tradeRegistryNumber(), entity.website(),
                entity.contactPhone(), entity.privateAddress(), entity.businessAddress(), entity.businessProfile());
    }
}
