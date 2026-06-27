package yowyob.comops.api.common.adapter.out.persistence;

import yowyob.comops.api.common.application.port.out.ContactRepository;
import yowyob.comops.api.common.domain.model.Contact;
import yowyob.comops.api.common.domain.model.ContactableType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("test-memory")
public class InMemoryContactRepository implements ContactRepository {

    private final Map<UUID, Contact> contacts = new ConcurrentHashMap<>();

    @Override
    public Mono<Contact> save(Contact contact) {
        return Mono.fromSupplier(() -> {
            contacts.put(contact.id(), contact);
            return contact;
        });
    }

    @Override
    public Flux<Contact> findByContactable(UUID tenantId, ContactableType contactableType, UUID contactableId) {
        return Flux.fromStream(contacts.values().stream()
                .filter(contact -> contact.tenantId().equals(tenantId))
                .filter(contact -> contact.contactableType() == contactableType)
                .filter(contact -> contact.contactableId().equals(contactableId))
                .filter(contact -> contact.deletedAt() == null));
    }

    @Override
    public Mono<Void> deleteById(UUID tenantId, UUID contactId) {
        return Mono.fromRunnable(() -> contacts.computeIfPresent(contactId,
                (id, contact) -> contact.tenantId().equals(tenantId) ? null : contact));
    }
}
