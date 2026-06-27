package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.application.port.in.GetOutboxObservabilityUseCase;
import yowyob.comops.api.kernel.application.port.in.GetProjectionObservabilityUseCase;
import yowyob.comops.api.kernel.application.service.KernelObservabilityApplicationService;
import yowyob.comops.api.kernel.domain.model.DomainEventProjection;
import yowyob.comops.api.kernel.domain.model.OutboxEvent;
import yowyob.comops.api.kernel.domain.model.OutboxEventStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final GetOutboxObservabilityUseCase getOutboxObservabilityUseCase;
    private final GetProjectionObservabilityUseCase getProjectionObservabilityUseCase;
    private final KernelObservabilityApplicationService kernelObservabilityApplicationService;

    public ObservabilityController(GetOutboxObservabilityUseCase getOutboxObservabilityUseCase,
            GetProjectionObservabilityUseCase getProjectionObservabilityUseCase,
            KernelObservabilityApplicationService kernelObservabilityApplicationService) {
        this.getOutboxObservabilityUseCase = getOutboxObservabilityUseCase;
        this.getProjectionObservabilityUseCase = getProjectionObservabilityUseCase;
        this.kernelObservabilityApplicationService = kernelObservabilityApplicationService;
    }

    @GetMapping("/outbox/events")
    @PreAuthorize("@businessAccessPolicy.hasPermission(authentication, 'system:observe')")
    public Mono<ResponseEntity<ApiResponse>> listOutboxEvents(@RequestParam("tenantId") UUID tenantId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        OutboxEventStatus parsedStatus = status == null || status.isBlank()
                ? null
                : OutboxEventStatus.valueOf(status.trim().toUpperCase());
        return getOutboxObservabilityUseCase.listEvents(tenantId, parsedStatus, limit)
                .map(this::toOutboxEventPayload)
                .collectList()
                .map(events -> ResponseEntity.ok(ApiResponse.success(events, "Outbox events retrieved.")));
    }

    @GetMapping("/outbox/summary")
    @PreAuthorize("@businessAccessPolicy.hasPermission(authentication, 'system:observe')")
    public Mono<ResponseEntity<ApiResponse>> summarizeOutbox(@RequestParam("tenantId") UUID tenantId) {
        return getOutboxObservabilityUseCase.summarizeOutbox(tenantId)
                .map(summary -> ResponseEntity.ok(ApiResponse.success(summary, "Outbox summary retrieved.")));
    }

    @GetMapping("/projections")
    @PreAuthorize("@businessAccessPolicy.hasPermission(authentication, 'system:observe')")
    public Mono<ResponseEntity<ApiResponse>> listProjections(@RequestParam("tenantId") UUID tenantId,
            @RequestParam(name = "domainType", required = false) String domainType,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        return getProjectionObservabilityUseCase.listProjections(tenantId, domainType, limit)
                .map(this::toProjectionPayload)
                .collectList()
                .map(projections -> ResponseEntity.ok(ApiResponse.success(projections, "Domain event projections retrieved.")));
    }

    @GetMapping("/projections/summary")
    @PreAuthorize("@businessAccessPolicy.hasPermission(authentication, 'system:observe')")
    public Mono<ResponseEntity<ApiResponse>> summarizeProjections(@RequestParam("tenantId") UUID tenantId) {
        return getProjectionObservabilityUseCase.summarizeProjections(tenantId)
                .map(summary -> ResponseEntity.ok(ApiResponse.success(summary, "Projection summary retrieved.")));
    }

    @GetMapping("/runtime")
    @PreAuthorize("@businessAccessPolicy.hasPermission(authentication, 'system:observe')")
    public Mono<ResponseEntity<ApiResponse>> runtime() {
        return kernelObservabilityApplicationService.runtime()
                .map(runtime -> ResponseEntity.ok(ApiResponse.success(runtime, "Runtime observability retrieved.")));
    }

    private Map<String, Object> toOutboxEventPayload(OutboxEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", event.id().toString());
        payload.put("tenantId", event.tenantId().toString());
        payload.put("organizationId", event.organizationId() == null ? null : event.organizationId().toString());
        payload.put("eventType", event.eventType());
        payload.put("aggregateType", event.aggregateType());
        payload.put("aggregateId", event.aggregateId().toString());
        payload.put("status", event.status().name());
        payload.put("attemptCount", event.attemptCount());
        payload.put("occurredAt", event.occurredAt());
        payload.put("lastAttemptAt", event.lastAttemptAt());
        payload.put("nextAttemptAt", event.nextAttemptAt());
        payload.put("lastError", event.lastError());
        payload.put("deadLetteredAt", event.deadLetteredAt());
        payload.put("publishedAt", event.publishedAt());
        payload.put("createdAt", event.createdAt());
        payload.put("updatedAt", event.updatedAt());
        payload.put("payload", event.payload());
        return payload;
    }

    private Map<String, Object> toProjectionPayload(DomainEventProjection projection) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", projection.id().toString());
        payload.put("tenantId", projection.tenantId().toString());
        payload.put("organizationId", projection.organizationId() == null ? null : projection.organizationId().toString());
        payload.put("domainType", projection.domainType());
        payload.put("eventType", projection.eventType());
        payload.put("aggregateType", projection.aggregateType());
        payload.put("aggregateId", projection.aggregateId().toString());
        payload.put("businessKey", projection.businessKey());
        payload.put("lifecycleStatus", projection.lifecycleStatus());
        payload.put("occurredAt", projection.occurredAt());
        payload.put("createdAt", projection.createdAt());
        payload.put("updatedAt", projection.updatedAt());
        payload.put("payload", projection.payload());
        return payload;
    }
}
