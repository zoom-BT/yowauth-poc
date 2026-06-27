package yowyob.comops.api.file.adapter.in.web;

import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.file.application.service.DocumentGovernanceApplicationService;
import yowyob.comops.api.file.domain.model.DocumentGovernancePolicy;
import yowyob.comops.api.file.domain.model.DocumentReview;
import yowyob.comops.api.kernel.application.service.ReactiveRequestContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("/api/document-governance")
@PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
public class DocumentGovernanceController {

    private final DocumentGovernanceApplicationService documentGovernanceApplicationService;

    public DocumentGovernanceController(DocumentGovernanceApplicationService documentGovernanceApplicationService) {
        this.documentGovernanceApplicationService = documentGovernanceApplicationService;
    }

    @PutMapping("/organizations/{organizationId}/policies/{targetType}/{documentCategory}")
    public Mono<ResponseEntity<ApiResponse<PolicyResponse>>> upsertOrganizationPolicy(@PathVariable UUID organizationId,
            @PathVariable String targetType, @PathVariable String documentCategory,
            @Valid @RequestBody Mono<UpsertPolicyRequest> requestMono) {
        return requestMono.zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> documentGovernanceApplicationService.upsertPolicy(tuple.getT2().tenantId(),
                        organizationId, null, targetType, documentCategory, tuple.getT1().toCommand()))
                .map(PolicyResponse::from)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Document governance policy updated.")));
    }

    @PutMapping("/organizations/{organizationId}/agencies/{agencyId}/policies/{targetType}/{documentCategory}")
    public Mono<ResponseEntity<ApiResponse<PolicyResponse>>> upsertAgencyPolicy(@PathVariable UUID organizationId,
            @PathVariable UUID agencyId, @PathVariable String targetType, @PathVariable String documentCategory,
            @Valid @RequestBody Mono<UpsertPolicyRequest> requestMono) {
        return requestMono.zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> documentGovernanceApplicationService.upsertPolicy(tuple.getT2().tenantId(),
                        organizationId, agencyId, targetType, documentCategory, tuple.getT1().toCommand()))
                .map(PolicyResponse::from)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Agency document governance policy updated.")));
    }

    @GetMapping("/targets/{targetType}/{targetId}")
    public Mono<ResponseEntity<ApiResponse<List<DocumentGovernanceApplicationService.DocumentStatusView>>>> targetStatus(
            @PathVariable String targetType, @PathVariable UUID targetId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> documentGovernanceApplicationService.targetStatus(context.tenantId(), targetType,
                                targetId)
                        .collectList())
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Document target status fetched.")));
    }

    @PostMapping("/documents/{documentLinkId}/reviews")
    public Mono<ResponseEntity<ApiResponse<ReviewResponse>>> review(@PathVariable UUID documentLinkId,
            @Valid @RequestBody Mono<ReviewDocumentRequest> requestMono) {
        return requestMono.zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> documentGovernanceApplicationService.review(tuple.getT2().tenantId(), documentLinkId,
                        tuple.getT2().userId(), tuple.getT1().reviewStatus(), tuple.getT1().expiresAt(),
                        tuple.getT1().notes()))
                .map(ReviewResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Document reviewed.")));
    }

    @GetMapping("/organizations/{organizationId}/overview")
    public Mono<ResponseEntity<ApiResponse<DocumentGovernanceApplicationService.DocumentGovernanceOverview>>> overview(
            @PathVariable UUID organizationId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> documentGovernanceApplicationService.organizationOverview(context.tenantId(),
                        organizationId))
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Document governance overview fetched.")));
    }

    public record UpsertPolicyRequest(boolean mandatory, boolean approvalRequired, @Min(0) Integer expiryDays,
            String reviewerResponsibilityType) {
        DocumentGovernanceApplicationService.UpsertDocumentGovernancePolicyCommand toCommand() {
            return new DocumentGovernanceApplicationService.UpsertDocumentGovernancePolicyCommand(mandatory,
                    approvalRequired, expiryDays, reviewerResponsibilityType);
        }
    }

    public record ReviewDocumentRequest(@NotBlank String reviewStatus, Instant expiresAt, String notes) {
    }

    public record PolicyResponse(UUID id, UUID organizationId, UUID agencyId, String targetType,
            String documentCategory, boolean mandatory, boolean approvalRequired, Integer expiryDays,
            String reviewerResponsibilityType) {
        static PolicyResponse from(DocumentGovernancePolicy policy) {
            return new PolicyResponse(policy.id(), policy.organizationId(), policy.agencyId(), policy.targetType(),
                    policy.documentCategory(), policy.mandatory(), policy.approvalRequired(), policy.expiryDays(),
                    policy.reviewerResponsibilityType());
        }
    }

    public record ReviewResponse(UUID id, UUID organizationId, UUID documentLinkId, UUID reviewerUserId,
            String reviewStatus, Instant reviewedAt, Instant expiresAt, String notes) {
        static ReviewResponse from(DocumentReview review) {
            return new ReviewResponse(review.id(), review.organizationId(), review.documentLinkId(),
                    review.reviewerUserId(), review.reviewStatus(), review.reviewedAt(), review.expiresAt(),
                    review.notes());
        }
    }
}
