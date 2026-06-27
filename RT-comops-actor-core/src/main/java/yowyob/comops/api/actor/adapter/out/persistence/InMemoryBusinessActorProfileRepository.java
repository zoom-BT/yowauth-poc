package yowyob.comops.api.actor.adapter.out.persistence;

import yowyob.comops.api.actor.application.port.out.BusinessActorProfileRepository;
import yowyob.comops.api.actor.domain.model.BusinessActorProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryBusinessActorProfileRepository implements BusinessActorProfileRepository {

    private final Map<UUID, BusinessActorProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public Mono<BusinessActorProfile> findById(UUID tenantId, UUID businessActorProfileId) {
        return Mono.justOrEmpty(profiles.get(businessActorProfileId))
                .filter(profile -> profile.tenantId().equals(tenantId));
    }

    @Override
    public Mono<BusinessActorProfile> findByActorId(UUID tenantId, UUID actorId) {
        return Mono.justOrEmpty(profiles.values().stream()
                .filter(profile -> profile.tenantId().equals(tenantId))
                .filter(profile -> profile.actorId().equals(actorId))
                .findFirst());
    }

    @Override
    public Flux<BusinessActorProfile> findByTenantId(UUID tenantId) {
        return Flux.fromStream(profiles.values().stream()
                .filter(profile -> profile.tenantId().equals(tenantId)));
    }

    @Override
    public Mono<BusinessActorProfile> save(BusinessActorProfile profile) {
        return Mono.fromSupplier(() -> {
            profiles.put(profile.id(), profile);
            return profile;
        });
    }
}
