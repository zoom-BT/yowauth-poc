package yowyob.comops.api.common.application.port.out;

import yowyob.comops.api.common.domain.model.Contact;
import yowyob.comops.api.common.domain.model.ContactableType;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ContactRepository {

    Mono<Contact> save(Contact contact);

    Flux<Contact> findByContactable(UUID tenantId, ContactableType contactableType, UUID contactableId);

    Mono<Void> deleteById(UUID tenantId, UUID contactId);
}
