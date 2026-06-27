package yowyob.comops.api.common.application.service;

import yowyob.comops.api.common.application.port.in.CreateAddressCommand;
import yowyob.comops.api.common.application.port.in.CreateAddressUseCase;
import yowyob.comops.api.common.application.port.in.CreateContactCommand;
import yowyob.comops.api.common.application.port.in.CreateContactUseCase;
import yowyob.comops.api.common.application.port.in.DeleteAddressUseCase;
import yowyob.comops.api.common.application.port.in.DeleteContactUseCase;
import yowyob.comops.api.common.application.port.in.ListAddressesUseCase;
import yowyob.comops.api.common.application.port.in.ListContactsUseCase;
import yowyob.comops.api.common.application.port.out.AddressRepository;
import yowyob.comops.api.common.application.port.out.ContactRepository;
import yowyob.comops.api.common.domain.model.Address;
import yowyob.comops.api.common.domain.model.Contact;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AddressContactApplicationService implements CreateAddressUseCase, ListAddressesUseCase, DeleteAddressUseCase,
        CreateContactUseCase, ListContactsUseCase, DeleteContactUseCase {

    private final AddressRepository addressRepository;
    private final ContactRepository contactRepository;

    public AddressContactApplicationService(AddressRepository addressRepository, ContactRepository contactRepository) {
        this.addressRepository = addressRepository;
        this.contactRepository = contactRepository;
    }

    @Override
    public Mono<Address> createAddress(CreateAddressCommand command) {
        Objects.requireNonNull(command, "command is required");
        Instant now = Instant.now();
        Address address = new Address(
                UUID.randomUUID(),
                command.tenantId(),
                command.addressableType(),
                command.addressableId(),
                command.type(),
                command.addressLine1(),
                command.addressLine2(),
                command.city(),
                command.state(),
                command.locality(),
                command.countryId(),
                command.zipCode(),
                command.postalCode(),
                command.poBox(),
                command.isDefault(),
                command.neighborhood(),
                command.informalDescription(),
                command.latitude(),
                command.longitude(),
                now,
                now,
                null);
        return addressRepository.save(address);
    }

    @Override
    public Flux<Address> listAddresses(UUID tenantId, yowyob.comops.api.common.domain.model.AddressableType addressableType,
            UUID addressableId) {
        return addressRepository.findByAddressable(tenantId, addressableType, addressableId);
    }

    @Override
    public Mono<Void> deleteAddress(UUID tenantId, UUID addressId) {
        return addressRepository.deleteById(tenantId, addressId);
    }

    @Override
    public Mono<Contact> createContact(CreateContactCommand command) {
        Objects.requireNonNull(command, "command is required");
        Instant now = Instant.now();
        Contact contact = new Contact(
                UUID.randomUUID(),
                command.tenantId(),
                command.contactableType(),
                command.contactableId(),
                command.firstName(),
                command.lastName(),
                command.title(),
                command.isEmailVerified(),
                command.isPhoneNumberVerified(),
                command.isFavorite(),
                command.phoneNumber(),
                command.secondaryPhoneNumber(),
                command.faxNumber(),
                command.email(),
                command.secondaryEmail(),
                command.isEmailVerified() ? now : null,
                command.isPhoneNumberVerified() ? now : null,
                now,
                now,
                null);
        return contactRepository.save(contact);
    }

    @Override
    public Flux<Contact> listContacts(UUID tenantId,
            yowyob.comops.api.common.domain.model.ContactableType contactableType,
            UUID contactableId) {
        return contactRepository.findByContactable(tenantId, contactableType, contactableId);
    }

    @Override
    public Mono<Void> deleteContact(UUID tenantId, UUID contactId) {
        return contactRepository.deleteById(tenantId, contactId);
    }
}
