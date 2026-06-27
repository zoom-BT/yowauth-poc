package yowyob.comops.api.actor.adapter.in.web;

import yowyob.comops.api.actor.application.port.out.ActorRepository;
import yowyob.comops.api.common.application.port.in.CreateAddressCommand;
import yowyob.comops.api.common.application.port.in.CreateAddressUseCase;
import yowyob.comops.api.common.application.port.in.CreateContactCommand;
import yowyob.comops.api.common.application.port.in.CreateContactUseCase;
import yowyob.comops.api.common.application.port.in.DeleteAddressUseCase;
import yowyob.comops.api.common.application.port.in.DeleteContactUseCase;
import yowyob.comops.api.common.application.port.in.ListAddressesUseCase;
import yowyob.comops.api.common.application.port.in.ListContactsUseCase;
import yowyob.comops.api.common.adapter.in.web.AddressResponse;
import yowyob.comops.api.common.adapter.in.web.ContactResponse;
import yowyob.comops.api.common.domain.model.AddressType;
import yowyob.comops.api.common.domain.model.AddressableType;
import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.common.domain.model.ContactableType;
import yowyob.comops.api.kernel.application.service.ReactiveRequestContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("/api/actors/{actorId}")
@PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
public class ActorAddressBookController {

    private final ActorRepository actorRepository;
    private final CreateAddressUseCase createAddressUseCase;
    private final ListAddressesUseCase listAddressesUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final CreateContactUseCase createContactUseCase;
    private final ListContactsUseCase listContactsUseCase;
    private final DeleteContactUseCase deleteContactUseCase;

    public ActorAddressBookController(ActorRepository actorRepository,
            CreateAddressUseCase createAddressUseCase,
            ListAddressesUseCase listAddressesUseCase,
            DeleteAddressUseCase deleteAddressUseCase,
            CreateContactUseCase createContactUseCase,
            ListContactsUseCase listContactsUseCase,
            DeleteContactUseCase deleteContactUseCase) {
        this.actorRepository = actorRepository;
        this.createAddressUseCase = createAddressUseCase;
        this.listAddressesUseCase = listAddressesUseCase;
        this.deleteAddressUseCase = deleteAddressUseCase;
        this.createContactUseCase = createContactUseCase;
        this.listContactsUseCase = listContactsUseCase;
        this.deleteContactUseCase = deleteContactUseCase;
    }

    @PostMapping("/addresses")
    public Mono<ResponseEntity<ApiResponse<AddressResponse>>> createAddress(@PathVariable UUID actorId,
            @Valid @RequestBody Mono<NestedAddressRequest> requestMono) {
        return requestMono.zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> ensureActorExists(tuple.getT2().tenantId(), actorId)
                        .then(createAddressUseCase.createAddress(new CreateAddressCommand(
                                tuple.getT2().tenantId(),
                                AddressableType.ACTOR,
                                actorId,
                                tuple.getT1().type(),
                                tuple.getT1().addressLine1(),
                                tuple.getT1().addressLine2(),
                                tuple.getT1().city(),
                                tuple.getT1().state(),
                                tuple.getT1().locality(),
                                tuple.getT1().countryId(),
                                tuple.getT1().zipCode(),
                                tuple.getT1().postalCode(),
                                tuple.getT1().poBox(),
                                tuple.getT1().isDefault(),
                                tuple.getT1().neighborhood(),
                                tuple.getT1().informalDescription(),
                                tuple.getT1().latitude(),
                                tuple.getT1().longitude()))))
                .map(AddressResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Actor address created.")));
    }

    @GetMapping("/addresses")
    public Mono<ResponseEntity<ApiResponse<List<AddressResponse>>>> listAddresses(@PathVariable UUID actorId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> ensureActorExists(context.tenantId(), actorId)
                        .then(listAddressesUseCase.listAddresses(context.tenantId(), AddressableType.ACTOR, actorId)
                                .map(AddressResponse::from)
                                .collectList()))
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Actor addresses retrieved.")));
    }

    @DeleteMapping("/addresses/{addressId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAddress(@PathVariable UUID actorId, @PathVariable UUID addressId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> ensureActorExists(context.tenantId(), actorId)
                        .then(deleteAddressUseCase.deleteAddress(context.tenantId(), addressId)))
                .thenReturn(ResponseEntity.ok(ApiResponse.success(null, "Actor address deleted.")));
    }

    @PostMapping("/contacts")
    public Mono<ResponseEntity<ApiResponse<ContactResponse>>> createContact(@PathVariable UUID actorId,
            @Valid @RequestBody Mono<NestedContactRequest> requestMono) {
        return requestMono.zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> ensureActorExists(tuple.getT2().tenantId(), actorId)
                        .then(createContactUseCase.createContact(new CreateContactCommand(
                                tuple.getT2().tenantId(),
                                ContactableType.ACTOR,
                                actorId,
                                tuple.getT1().firstName(),
                                tuple.getT1().lastName(),
                                tuple.getT1().title(),
                                tuple.getT1().isEmailVerified(),
                                tuple.getT1().isPhoneNumberVerified(),
                                tuple.getT1().isFavorite(),
                                tuple.getT1().phoneNumber(),
                                tuple.getT1().secondaryPhoneNumber(),
                                tuple.getT1().faxNumber(),
                                tuple.getT1().email(),
                                tuple.getT1().secondaryEmail()))))
                .map(ContactResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Actor contact created.")));
    }

    @GetMapping("/contacts")
    public Mono<ResponseEntity<ApiResponse<List<ContactResponse>>>> listContacts(@PathVariable UUID actorId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> ensureActorExists(context.tenantId(), actorId)
                        .then(listContactsUseCase.listContacts(context.tenantId(), ContactableType.ACTOR, actorId)
                                .map(ContactResponse::from)
                                .collectList()))
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Actor contacts retrieved.")));
    }

    @DeleteMapping("/contacts/{contactId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteContact(@PathVariable UUID actorId, @PathVariable UUID contactId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> ensureActorExists(context.tenantId(), actorId)
                        .then(deleteContactUseCase.deleteContact(context.tenantId(), contactId)))
                .thenReturn(ResponseEntity.ok(ApiResponse.success(null, "Actor contact deleted.")));
    }

    private Mono<Void> ensureActorExists(UUID tenantId, UUID actorId) {
        return actorRepository.findById(tenantId, actorId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("actor not found")))
                .then();
    }

    record NestedAddressRequest(
            @NotNull AddressType type,
            @NotBlank String addressLine1,
            String addressLine2,
            String city,
            String state,
            String locality,
            UUID countryId,
            String zipCode,
            String postalCode,
            String poBox,
            boolean isDefault,
            String neighborhood,
            String informalDescription,
            Double latitude,
            Double longitude) {
    }

    record NestedContactRequest(
            String firstName,
            String lastName,
            String title,
            boolean isEmailVerified,
            boolean isPhoneNumberVerified,
            boolean isFavorite,
            String phoneNumber,
            String secondaryPhoneNumber,
            String faxNumber,
            String email,
            String secondaryEmail) {
    }
}
