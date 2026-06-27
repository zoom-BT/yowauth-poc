package yowyob.comops.api.common.adapter.in.web;

import yowyob.comops.api.common.application.port.in.CreateAddressCommand;
import yowyob.comops.api.common.application.port.in.CreateAddressUseCase;
import yowyob.comops.api.common.application.port.in.DeleteAddressUseCase;
import yowyob.comops.api.common.application.port.in.ListAddressesUseCase;
import yowyob.comops.api.common.domain.model.AddressableType;
import yowyob.comops.api.common.domain.model.ApiResponse;
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
@RequestMapping("/api/addresses")
public class AddressController {

    private final CreateAddressUseCase createAddressUseCase;
    private final ListAddressesUseCase listAddressesUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;

    public AddressController(CreateAddressUseCase createAddressUseCase, ListAddressesUseCase listAddressesUseCase,
            DeleteAddressUseCase deleteAddressUseCase) {
        this.createAddressUseCase = createAddressUseCase;
        this.listAddressesUseCase = listAddressesUseCase;
        this.deleteAddressUseCase = deleteAddressUseCase;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<AddressResponse>>> createAddress(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody Mono<CreateAddressRequest> requestMono) {
        return requestMono.flatMap(request -> createAddressUseCase.createAddress(new CreateAddressCommand(
                        tenantId, request.addressableType(), request.addressableId(),
                        request.type(), request.addressLine1(), request.addressLine2(),
                        request.city(), request.state(), request.locality(), request.countryId(),
                        request.zipCode(), request.postalCode(), request.poBox(),
                        request.isDefault(), request.neighborhood(), request.informalDescription(),
                        request.latitude(), request.longitude())))
                .map(AddressResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Address created.")));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<AddressResponse>>>> listAddresses(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam("addressableType") AddressableType addressableType,
            @RequestParam("addressableId") UUID addressableId) {
        return listAddressesUseCase.listAddresses(tenantId, addressableType, addressableId)
                .map(AddressResponse::from)
                .collectList()
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Addresses retrieved.")));
    }

    @DeleteMapping("/{addressId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAddress(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("addressId") UUID addressId) {
        return deleteAddressUseCase.deleteAddress(tenantId, addressId)
                .thenReturn(ResponseEntity.ok(ApiResponse.success(null, "Address deleted.")));
    }
}
