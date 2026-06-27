package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.kernel.application.port.out.ClientApplicationRepository;
import yowyob.comops.api.kernel.domain.model.ClientApplication;
import yowyob.comops.api.kernel.domain.model.ClientApplicationStatus;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class ClientApplicationR2dbcRepositoryAdapter implements ClientApplicationRepository {

    private final ClientApplicationSpringDataRepository repository;

    public ClientApplicationR2dbcRepositoryAdapter(ClientApplicationSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Boolean> existsByClientId(String clientId) {
        return repository.existsByClientIdIgnoreCase(clientId);
    }

    @Override
    public Mono<ClientApplication> findById(UUID clientApplicationId) {
        return repository.findById(clientApplicationId).map(this::toDomain);
    }

    @Override
    public Mono<ClientApplication> findByClientId(String clientId) {
        return repository.findByClientIdIgnoreCase(clientId).map(this::toDomain);
    }

    @Override
    public Flux<ClientApplication> findAll() {
        return repository.findAll().map(this::toDomain);
    }

    @Override
    public Mono<ClientApplication> save(ClientApplication clientApplication) {
        return repository.save(toEntity(clientApplication)).map(this::toDomain);
    }

    private ClientApplicationEntity toEntity(ClientApplication clientApplication) {
        return new ClientApplicationEntity(
                clientApplication.id(),
                clientApplication.createdAt(),
                clientApplication.updatedAt(),
                clientApplication.clientId(),
                clientApplication.name(),
                clientApplication.description(),
                clientApplication.secretHash(),
                clientApplication.status().name(),
                clientApplication.systemManaged(),
                clientApplication.allowedServiceCodes().toArray(String[]::new),
                clientApplication.lastAuthenticatedAt(),
                clientApplication.secretRotatedAt());
    }

    private ClientApplication toDomain(ClientApplicationEntity entity) {
        return ClientApplication.rehydrate(entity.id(), entity.createdAt(), entity.updatedAt(), entity.clientId(),
                entity.name(), entity.description(), entity.secretHash(),
                ClientApplicationStatus.valueOf(entity.status()), entity.systemManaged(),
                entity.allowedServiceCodes() == null
                        ? java.util.Set.of()
                        : new LinkedHashSet<>(Arrays.asList(entity.allowedServiceCodes())),
                entity.lastAuthenticatedAt(), entity.secretRotatedAt());
    }
}
