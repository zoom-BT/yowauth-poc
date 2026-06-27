package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import yowyob.comops.api.kernel.application.port.out.ExternalServiceRepository;
import yowyob.comops.api.kernel.domain.model.ExternalServiceDefinition;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class ExternalServiceR2dbcRepositoryAdapter implements ExternalServiceRepository {

    private final ExternalServiceSpringDataRepository repository;

    public ExternalServiceR2dbcRepositoryAdapter(ExternalServiceSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Flux<ExternalServiceDefinition> findAll() {
        return repository.findAll().map(this::toDomain);
    }

    @Override
    public Mono<ExternalServiceDefinition> save(ExternalServiceDefinition definition) {
        String code = PlatformServiceCode.normalizeCode(definition.code());
        Instant now = Instant.now();
        return repository.findByCodeIgnoreCase(code)
                .flatMap(existing -> repository.save(new ExternalServiceEntity(existing.id(), existing.createdAt(), now,
                        code, definition.displayName(), definition.description(), definition.active())))
                .switchIfEmpty(Mono.defer(() -> repository.save(new ExternalServiceEntity(UUID.randomUUID(), now, now,
                        code, definition.displayName(), definition.description(), definition.active()))))
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deactivate(String code) {
        String normalized = PlatformServiceCode.normalizeCode(code);
        return repository.findByCodeIgnoreCase(normalized)
                .flatMap(existing -> repository.save(new ExternalServiceEntity(existing.id(), existing.createdAt(),
                        Instant.now(), existing.code(), existing.displayName(), existing.description(), false)))
                .then();
    }

    private ExternalServiceDefinition toDomain(ExternalServiceEntity entity) {
        return new ExternalServiceDefinition(entity.code(), entity.displayName(), entity.description(), entity.active());
    }
}
