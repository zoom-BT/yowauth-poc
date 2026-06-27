package yowyob.comops.api.common.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import yowyob.comops.api.common.adapter.out.persistence.InMemoryAddressRepository;
import yowyob.comops.api.common.adapter.out.persistence.InMemoryContactRepository;
import yowyob.comops.api.common.application.port.in.CreateAddressCommand;
import yowyob.comops.api.common.application.port.in.CreateContactCommand;
import yowyob.comops.api.common.domain.model.Address;
import yowyob.comops.api.common.domain.model.AddressType;
import yowyob.comops.api.common.domain.model.AddressableType;
import yowyob.comops.api.common.domain.model.Contact;
import yowyob.comops.api.common.domain.model.ContactableType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AddressContactApplicationServiceTest {

    private final AddressContactApplicationService service = new AddressContactApplicationService(
            new InMemoryAddressRepository(), new InMemoryContactRepository());

    @Test
    void createsAndListsPolymorphicAddressesAndContacts() {
        UUID tenantId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        Address address = service.createAddress(new CreateAddressCommand(
                tenantId, AddressableType.ORGANIZATION, organizationId, AddressType.HEADQUARTER, "Rue 1", null, "Douala",
                null, null, null, null, null, null, true, null, null, null, null)).block();

        Contact contact = service.createContact(new CreateContactCommand(
                tenantId, ContactableType.ORGANIZATION, organizationId, "Alice", "Admin", "CEO",
                false, false, true, "+237600000000", null, null, "alice@example.com", null)).block();

        assertThat(address).isNotNull();
        assertThat(contact).isNotNull();

        assertThat(service.listAddresses(tenantId, AddressableType.ORGANIZATION, organizationId).collectList().block())
                .singleElement()
                .extracting(Address::addressLine1)
                .isEqualTo("Rue 1");

        assertThat(service.listContacts(tenantId, ContactableType.ORGANIZATION, organizationId).collectList().block())
                .singleElement()
                .extracting(Contact::email)
                .isEqualTo("alice@example.com");
    }
}
