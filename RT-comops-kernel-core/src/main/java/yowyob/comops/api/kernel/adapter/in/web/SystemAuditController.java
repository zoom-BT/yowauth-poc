package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.application.port.in.ListSystemAuditUseCase;
import yowyob.comops.api.kernel.application.port.in.VerifyAuditIntegrityUseCase;
import yowyob.comops.api.kernel.application.service.ReactiveRequestContextHolder;
import yowyob.comops.api.kernel.domain.model.SystemAuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/system-audits")
public class SystemAuditController {

    private final ListSystemAuditUseCase listSystemAuditUseCase;
    private final VerifyAuditIntegrityUseCase verifyAuditIntegrityUseCase;

    public SystemAuditController(ListSystemAuditUseCase listSystemAuditUseCase,
            VerifyAuditIntegrityUseCase verifyAuditIntegrityUseCase) {
        this.listSystemAuditUseCase = listSystemAuditUseCase;
        this.verifyAuditIntegrityUseCase = verifyAuditIntegrityUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
    public Mono<ResponseEntity<ApiResponse<List<SystemAuditResponse>>>> getMyActivity(
            @RequestParam(defaultValue = "50") int limit) {
        return listSystemAuditUseCase.listCurrentUserActivity(limit)
                .map(SystemAuditResponse::from)
                .collectList()
                .map(response -> ResponseEntity.ok(ApiResponse.success(response, "User system audit retrieved.")));
    }

    @GetMapping("/organization")
    @PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
    public Mono<ResponseEntity<ApiResponse<List<SystemAuditResponse>>>> getOrganizationActivity(
            @RequestParam(defaultValue = "50") int limit) {
        return listSystemAuditUseCase.listCurrentOrganizationActivity(limit)
                .map(SystemAuditResponse::from)
                .collectList()
                .map(response -> ResponseEntity.ok(ApiResponse.success(response,
                        "Organization system audit retrieved.")));
    }

    @GetMapping("/organization/search")
    @PreAuthorize("@businessAccessPolicy.hasUserContext(authentication)")
    public Mono<ResponseEntity<ApiResponse<yowyob.comops.api.kernel.application.port.in.ListSystemAuditUseCase.AuditPage>>> searchOrganizationActivity(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) java.util.UUID actorUserId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.Instant from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) java.util.UUID organizationId) {
        return listSystemAuditUseCase.searchOrganizationActivity(organizationId, action, actorUserId, from, to, page, size)
                .map(result -> ResponseEntity.ok(ApiResponse.success(result, "Audit search completed.")));
    }

    @GetMapping("/integrity-check")
    @PreAuthorize("hasAuthority('SYSTEM:ADMIN') or hasAuthority('TENANT:ADMIN') or hasAuthority('IAM:ADMIN')")
    public Mono<ResponseEntity<ApiResponse<VerifyAuditIntegrityUseCase.IntegrityReport>>> verifyIntegrity(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "1000") int maxScan,
            @RequestParam(required = false) UUID organizationId) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(ctx -> verifyAuditIntegrityUseCase.verify(ctx.tenantId(),
                        organizationId == null ? ctx.organizationId() : organizationId,
                        from, to, maxScan))
                .map(report -> {
                    String msg = report.tampered() == 0
                            ? "Audit integrity OK. " + report.scanned() + " entries scanned."
                            : "AUDIT INTEGRITY VIOLATION: " + report.tampered() + " tampered entries detected.";
                    return ResponseEntity.ok(ApiResponse.success(report, msg));
                });
    }

    public record SystemAuditResponse(UUID id, UUID tenantId, UUID organizationId, UUID actorUserId, String action,
            String targetType, String targetId, String payloadSummary, Instant createdAt,
            String requestId, String clientApplicationId, String remoteIp, String httpMethod, String httpPath,
            String integrityHash) {
        public static SystemAuditResponse from(SystemAuditEntry entry) {
            return new SystemAuditResponse(entry.id(), entry.tenantId(), entry.organizationId(), entry.actorUserId(),
                    entry.action(), entry.targetType(), entry.targetId(), entry.payloadSummary(), entry.createdAt(),
                    entry.requestId(), entry.clientApplicationId(), entry.remoteIp(), entry.httpMethod(),
                    entry.httpPath(), entry.integrityHash());
        }
    }
}
