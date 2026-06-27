package yowyob.comops.api.actor.adapter.in.web;

import yowyob.comops.api.actor.application.port.in.CreateActorCommand;
import yowyob.comops.api.actor.application.port.in.CreateActorUseCase;
import yowyob.comops.api.actor.application.port.in.GetCurrentBusinessActorUseCase;
import yowyob.comops.api.actor.application.port.in.OnboardBusinessActorCommand;
import yowyob.comops.api.actor.application.port.in.OnboardBusinessActorUseCase;
import yowyob.comops.api.actor.application.port.in.ReactivateMyBusinessActorCommand;
import yowyob.comops.api.actor.application.port.in.ReactivateMyBusinessActorUseCase;
import yowyob.comops.api.actor.application.port.in.UpdateBusinessActorCommand;
import yowyob.comops.api.actor.application.port.in.UpdateBusinessActorUseCase;
import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.application.service.ReactiveRequestContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("/api/actors")
public class ActorController {

    private final CreateActorUseCase createActorUseCase;
    private final OnboardBusinessActorUseCase onboardBusinessActorUseCase;
    private final GetCurrentBusinessActorUseCase getCurrentBusinessActorUseCase;
    private final UpdateBusinessActorUseCase updateBusinessActorUseCase;
    private final ReactivateMyBusinessActorUseCase reactivateMyBusinessActorUseCase;

    public ActorController(CreateActorUseCase createActorUseCase,
            OnboardBusinessActorUseCase onboardBusinessActorUseCase,
            GetCurrentBusinessActorUseCase getCurrentBusinessActorUseCase,
            UpdateBusinessActorUseCase updateBusinessActorUseCase,
            ReactivateMyBusinessActorUseCase reactivateMyBusinessActorUseCase) {
        this.createActorUseCase = createActorUseCase;
        this.onboardBusinessActorUseCase = onboardBusinessActorUseCase;
        this.getCurrentBusinessActorUseCase = getCurrentBusinessActorUseCase;
        this.updateBusinessActorUseCase = updateBusinessActorUseCase;
        this.reactivateMyBusinessActorUseCase = reactivateMyBusinessActorUseCase;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<ActorResponse>>> createActor(@Valid @RequestBody Mono<CreateActorRequest> requestMono) {
        return requestMono
                .zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> createActorUseCase.createActor(new CreateActorCommand(
                        tuple.getT2().tenantId(),
                        tuple.getT1().organizationId(),
                        tuple.getT1().firstName(),
                        tuple.getT1().lastName(),
                        tuple.getT1().name(),
                        tuple.getT1().phoneNumber(),
                        tuple.getT1().email(),
                        tuple.getT1().description(),
                        tuple.getT1().type(),
                        tuple.getT1().gender(),
                        tuple.getT1().photoUri(),
                        tuple.getT1().photoId(),
                        tuple.getT1().nationality(),
                        tuple.getT1().birthDate(),
                        tuple.getT1().profession(),
                        tuple.getT1().biography(),
                        tuple.getT1().addresses(),
                        tuple.getT1().contacts())))
                .map(ActorResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Actor created.")));
    }

    @PostMapping("/onboarding")
    @PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
    public Mono<ResponseEntity<ApiResponse<BusinessActorResponse>>> onboardBusinessActor(
            @Valid @RequestBody Mono<BusinessActorRequest> requestMono) {
        return requestMono.zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> onboardBusinessActorUseCase.onboard(new OnboardBusinessActorCommand(
                        tuple.getT2().tenantId(),
                        tuple.getT2().userId(),
                        tuple.getT1().code(),
                        tuple.getT1().resolvedIndividual(),
                        tuple.getT1().resolvedAvailable(),
                        tuple.getT1().resolvedVerified(),
                        tuple.getT1().resolvedActive(),
                        tuple.getT1().type(),
                        tuple.getT1().role(),
                        tuple.getT1().qualifications(),
                        tuple.getT1().paymentMethods(),
                        tuple.getT1().addresses(),
                        tuple.getT1().resolvedBiography(),
                        tuple.getT1().name(),
                        tuple.getT1().businessId(),
                        tuple.getT1().niu(),
                        tuple.getT1().tradeRegistryNumber(),
                        tuple.getT1().website(),
                        tuple.getT1().contactPhone(),
                        tuple.getT1().privateAddress(),
                        tuple.getT1().businessAddress(),
                        tuple.getT1().businessProfile())))
                .map(BusinessActorResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Business actor onboarded.")));
    }

    @GetMapping("/me")
    @PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
    public Mono<ResponseEntity<ApiResponse<BusinessActorResponse>>> getMyBusinessActorProfile() {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> getCurrentBusinessActorUseCase.getByUser(context.tenantId(), context.userId()))
                .map(BusinessActorResponse::from)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Business actor profile retrieved.")));
    }

    @PutMapping("/me")
    @PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
    public Mono<ResponseEntity<ApiResponse<BusinessActorResponse>>> updateMyBusinessActorProfile(
            @Valid @RequestBody Mono<BusinessActorRequest> requestMono) {
        return requestMono.zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> updateBusinessActorUseCase.update(new UpdateBusinessActorCommand(
                        tuple.getT2().tenantId(),
                        tuple.getT2().userId(),
                        tuple.getT1().code(),
                        tuple.getT1().resolvedIndividual(),
                        tuple.getT1().resolvedAvailable(),
                        tuple.getT1().resolvedVerified(),
                        tuple.getT1().resolvedActive(),
                        tuple.getT1().type(),
                        tuple.getT1().role(),
                        tuple.getT1().qualifications(),
                        tuple.getT1().paymentMethods(),
                        tuple.getT1().addresses(),
                        tuple.getT1().resolvedBiography(),
                        tuple.getT1().name(),
                        tuple.getT1().businessId(),
                        tuple.getT1().niu(),
                        tuple.getT1().tradeRegistryNumber(),
                        tuple.getT1().website(),
                        tuple.getT1().contactPhone(),
                        tuple.getT1().privateAddress(),
                        tuple.getT1().businessAddress(),
                        tuple.getT1().businessProfile())))
                .map(BusinessActorResponse::from)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Business actor profile updated.")));
    }

    @PostMapping("/me/reactivate")
    @PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
    public Mono<ResponseEntity<ApiResponse<BusinessActorResponse>>> reactivateMyBusinessActorProfile() {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> reactivateMyBusinessActorUseCase.reactivate(
                        new ReactivateMyBusinessActorCommand(context.tenantId(), context.userId())))
                .map(BusinessActorResponse::from)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Business actor profile reactivated.")));
    }
}
