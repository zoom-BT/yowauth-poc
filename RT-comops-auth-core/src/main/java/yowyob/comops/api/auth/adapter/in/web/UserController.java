package yowyob.comops.api.auth.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import yowyob.comops.api.auth.application.port.in.GetCurrentUserProfileUseCase;
import yowyob.comops.api.auth.application.port.in.UpdateCurrentUserOnboardingUseCase;
import yowyob.comops.api.auth.application.port.in.UpdateCurrentUserPlanUseCase;
import yowyob.comops.api.auth.application.service.AuthApplicationService;
import yowyob.comops.api.common.domain.model.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("/api/users")
@PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
public class UserController {

    private final GetCurrentUserProfileUseCase getCurrentUserProfileUseCase;
    private final UpdateCurrentUserPlanUseCase updateCurrentUserPlanUseCase;
    private final UpdateCurrentUserOnboardingUseCase updateCurrentUserOnboardingUseCase;
    private final AuthApplicationService authApplicationService;
    private final AuthUserViewAssembler authUserViewAssembler;
    private final ObjectMapper objectMapper;

    public UserController(GetCurrentUserProfileUseCase getCurrentUserProfileUseCase,
            UpdateCurrentUserPlanUseCase updateCurrentUserPlanUseCase,
            UpdateCurrentUserOnboardingUseCase updateCurrentUserOnboardingUseCase,
            AuthApplicationService authApplicationService,
            AuthUserViewAssembler authUserViewAssembler,
            ObjectMapper objectMapper) {
        this.getCurrentUserProfileUseCase = getCurrentUserProfileUseCase;
        this.updateCurrentUserPlanUseCase = updateCurrentUserPlanUseCase;
        this.updateCurrentUserOnboardingUseCase = updateCurrentUserOnboardingUseCase;
        this.authApplicationService = authApplicationService;
        this.authUserViewAssembler = authUserViewAssembler;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<ApiResponse<UserAccountResponse>>> getMe() {
        return getCurrentUserProfileUseCase.getCurrentUserProfile()
                .flatMap(authUserViewAssembler::toUserAccountResponse)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Current user profile retrieved.")));
    }

    @PutMapping("/me/plan")
    public Mono<ResponseEntity<ApiResponse<UserAccountResponse>>> updateMyPlan(
            @Valid @RequestBody Mono<UpdatePlanRequest> requestMono) {
        return requestMono.flatMap(request -> updateCurrentUserPlanUseCase.updateCurrentUserPlan(request.plan()))
                .flatMap(authUserViewAssembler::toUserAccountResponse)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "User plan updated.")));
    }

    @PutMapping("/me/onboarding")
    public Mono<ResponseEntity<ApiResponse<UserAccountResponse>>> updateOnboarding(
            @Valid @RequestBody Mono<UpdateOnboardingRequest> requestMono) {
        return requestMono.flatMap(request -> updateCurrentUserOnboardingUseCase
                        .updateCurrentUserOnboarding(request.step(), request.status()))
                .flatMap(authUserViewAssembler::toUserAccountResponse)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "User onboarding updated.")));
    }

    @PutMapping("/me/identity-onboarding")
    public Mono<ResponseEntity<ApiResponse<UserAccountResponse>>> updateIdentityOnboarding(
            @Valid @RequestBody Mono<UpdateIdentityOnboardingRequest> requestMono) {
        return requestMono.flatMap(request -> authApplicationService.updateCurrentIdentityOnboarding(
                        request.accountType(),
                        request.businessType(),
                        toJson(request.data()),
                        request.step(),
                        request.status()))
                .flatMap(authUserViewAssembler::toUserAccountResponse)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Identity onboarding updated.")));
    }

    public record UpdatePlanRequest(@NotBlank String plan) {
    }

    public record UpdateOnboardingRequest(@Min(0) int step, String status) {
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid onboarding data.", exception);
        }
    }
}
