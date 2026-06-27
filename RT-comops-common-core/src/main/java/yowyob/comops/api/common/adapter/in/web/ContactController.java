package yowyob.comops.api.common.adapter.in.web;

import yowyob.comops.api.common.application.port.in.CreateContactCommand;
import yowyob.comops.api.common.application.port.in.CreateContactUseCase;
import yowyob.comops.api.common.application.port.in.DeleteContactUseCase;
import yowyob.comops.api.common.application.port.in.ListContactsUseCase;
import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.common.domain.model.ContactableType;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("/api/contacts")
public class ContactController {

    private final CreateContactUseCase createContactUseCase;
    private final ListContactsUseCase listContactsUseCase;
    private final DeleteContactUseCase deleteContactUseCase;

    public ContactController(CreateContactUseCase createContactUseCase, ListContactsUseCase listContactsUseCase,
            DeleteContactUseCase deleteContactUseCase) {
        this.createContactUseCase = createContactUseCase;
        this.listContactsUseCase = listContactsUseCase;
        this.deleteContactUseCase = deleteContactUseCase;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<ContactResponse>>> createContact(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody Mono<CreateContactRequest> requestMono) {
        return requestMono.flatMap(request -> createContactUseCase.createContact(new CreateContactCommand(
                        tenantId, request.contactableType(), request.contactableId(),
                        request.firstName(), request.lastName(), request.title(),
                        request.isEmailVerified(), request.isPhoneNumberVerified(),
                        request.isFavorite(), request.phoneNumber(), request.secondaryPhoneNumber(),
                        request.faxNumber(), request.email(), request.secondaryEmail())))
                .map(ContactResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Contact created.")));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<ContactResponse>>>> listContacts(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam("contactableType") ContactableType contactableType,
            @RequestParam("contactableId") UUID contactableId) {
        return listContactsUseCase.listContacts(tenantId, contactableType, contactableId)
                .map(ContactResponse::from)
                .collectList()
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Contacts retrieved.")));
    }

    @DeleteMapping("/{contactId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteContact(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("contactId") UUID contactId) {
        return deleteContactUseCase.deleteContact(tenantId, contactId)
                .thenReturn(ResponseEntity.ok(ApiResponse.success(null, "Contact deleted.")));
    }
}
