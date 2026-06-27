package yowyob.comops.api.common.application.port.in;

import yowyob.comops.api.common.domain.model.Contact;
import reactor.core.publisher.Mono;

public interface CreateContactUseCase {

    Mono<Contact> createContact(CreateContactCommand command);
}
