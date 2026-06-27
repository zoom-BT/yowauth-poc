package yowyob.comops.api.file.adapter.in.web;

import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.file.application.service.DocumentHubApplicationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("/api/document-hub")
@PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
public class DocumentHubController {

    private final DocumentHubApplicationService documentHubApplicationService;

    public DocumentHubController(DocumentHubApplicationService documentHubApplicationService) {
        this.documentHubApplicationService = documentHubApplicationService;
    }

    @PostMapping("/links")
    public Mono<ResponseEntity<ApiResponse<DocumentHubApplicationService.DocumentLinkView>>> attachDocument(
            @Valid @RequestBody Mono<AttachDocumentRequest> requestMono) {
        return requestMono.zipWith(ReactiveRequestContextHolder.getRequiredContext())
                .flatMap(tuple -> documentHubApplicationService.attach(tuple.getT2().tenantId(),
                        tuple.getT2().organizationId(), tuple.getT2().userId(), tuple.getT1().targetType(),
                        tuple.getT1().targetId(), tuple.getT1().fileId(), tuple.getT1().documentCategory(),
                        tuple.getT1().label()))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Document linked.")));
    }

    @GetMapping("/targets/{targetType}/{targetId}")
    public Mono<ResponseEntity<ApiResponse<List<DocumentHubApplicationService.DocumentLinkView>>>> listTargetDocuments(
            @PathVariable String targetType, @PathVariable UUID targetId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> documentHubApplicationService.listByTarget(context.tenantId(), targetType, targetId)
                        .collectList())
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Documents fetched.")));
    }

    @GetMapping("/organizations/{organizationId}")
    public Mono<ResponseEntity<ApiResponse<List<DocumentHubApplicationService.DocumentLinkView>>>> listOrganizationDocuments(
            @PathVariable UUID organizationId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> documentHubApplicationService.listByOrganization(context.tenantId(), organizationId)
                        .collectList())
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Organization documents fetched.")));
    }

    @GetMapping("/organizations/{organizationId}/overview")
    public Mono<ResponseEntity<ApiResponse<DocumentHubApplicationService.DocumentHubOverview>>> overview(
            @PathVariable UUID organizationId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> documentHubApplicationService.overview(context.tenantId(), organizationId))
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "Document hub overview fetched.")));
    }

    public record AttachDocumentRequest(
            @NotBlank String targetType,
            @NotNull UUID targetId,
            @NotNull UUID fileId,
            @NotBlank String documentCategory,
            String label) {
    }
}
