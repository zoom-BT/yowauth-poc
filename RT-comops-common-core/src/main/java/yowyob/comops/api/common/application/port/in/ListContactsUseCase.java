package yowyob.comops.api.common.application.port.in;

import yowyob.comops.api.common.domain.model.Contact;
import yowyob.comops.api.common.domain.model.ContactableType;
import java.util.UUID;
import reactor.core.publisher.Flux;

public interface ListContactsUseCase {

    Flux<Contact> listContacts(UUID tenantId, ContactableType contactableType, UUID contactableId);
}
