package yowyob.comops.api.common.adapter.out.persistence;

import yowyob.comops.api.common.application.port.out.ContactRepository;
import yowyob.comops.api.common.domain.model.Contact;
import yowyob.comops.api.common.domain.model.ContactableType;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("r2dbc")
public class ContactR2dbcRepositoryAdapter implements ContactRepository {

    private final ContactSpringDataRepository repository;

    public ContactR2dbcRepositoryAdapter(ContactSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Contact> save(Contact contact) {
        return repository.save(toEntity(contact)).map(this::toDomain);
    }

    @Override
    public Flux<Contact> findByContactable(UUID tenantId, ContactableType contactableType, UUID contactableId) {
        return repository.findAllByTenantIdAndContactableTypeAndContactableIdAndDeletedAtIsNull(
                tenantId, contactableType.name(), contactableId).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(UUID tenantId, UUID contactId) {
        return repository.deleteByIdAndTenantId(contactId, tenantId);
    }

    private ContactEntity toEntity(Contact contact) {
        return new ContactEntity(contact.id(), contact.tenantId(), contact.createdAt(), contact.updatedAt(),
                contact.deletedAt(), contact.contactableType().name(), contact.contactableId(), contact.firstName(),
                contact.lastName(), contact.title(), contact.isEmailVerified(), contact.isPhoneNumberVerified(),
                contact.isFavorite(), contact.phoneNumber(), contact.secondaryPhoneNumber(), contact.faxNumber(),
                contact.email(), contact.secondaryEmail(), contact.emailVerifiedAt(), contact.phoneVerifiedAt());
    }

    private Contact toDomain(ContactEntity entity) {
        return new Contact(entity.id(), entity.tenantId(), ContactableType.valueOf(entity.contactableType()),
                entity.contactableId(), entity.firstName(), entity.lastName(), entity.title(),
                entity.isEmailVerified(), entity.isPhoneNumberVerified(), entity.isFavorite(), entity.phoneNumber(),
                entity.secondaryPhoneNumber(), entity.faxNumber(), entity.email(), entity.secondaryEmail(),
                entity.emailVerifiedAt(), entity.phoneVerifiedAt(), entity.createdAt(), entity.updatedAt(),
                entity.deletedAt());
    }
}
