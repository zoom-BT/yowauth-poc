package yowyob.comops.api.kernel.adapter.out.persistence;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import yowyob.comops.api.kernel.application.port.out.ExternalServiceRepository;
import yowyob.comops.api.kernel.domain.model.ExternalServiceDefinition;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!r2dbc")
public class InMemoryExternalServiceRepository implements ExternalServiceRepository {

    private final Map<String, ExternalServiceDefinition> storage = new ConcurrentHashMap<>();

    @Override
    public Flux<ExternalServiceDefinition> findAll() {
        return Flux.fromIterable(storage.values()).sort(Comparator.comparing(ExternalServiceDefinition::code));
    }

    @Override
    public Mono<ExternalServiceDefinition> save(ExternalServiceDefinition definition) {
        String code = PlatformServiceCode.normalizeCode(definition.code());
        ExternalServiceDefinition stored = new ExternalServiceDefinition(code, definition.displayName(),
                definition.description(), definition.active());
        storage.put(code, stored);
        return Mono.just(stored);
    }

    @Override
    public Mono<Void> deactivate(String code) {
        String normalized = PlatformServiceCode.normalizeCode(code);
        ExternalServiceDefinition existing = storage.get(normalized);
        if (existing != null) {
            storage.put(normalized, new ExternalServiceDefinition(existing.code(), existing.displayName(),
                    existing.description(), false));
        }
        return Mono.empty();
    }
}
