package yowyob.comops.api.kernel.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SystemAuditEntrySpringDataRepository extends ReactiveCrudRepository<SystemAuditEntryEntity, UUID> {

    Flux<SystemAuditEntryEntity> findTop200ByTenantIdAndActorUserIdOrderByCreatedAtDesc(UUID tenantId, UUID actorUserId);
    Flux<SystemAuditEntryEntity> findTop200ByTenantIdAndOrganizationIdOrderByCreatedAtDesc(UUID tenantId, UUID organizationId);

    @Query("""
            SELECT * FROM kernel.system_audit_entry
            WHERE tenant_id = :tenantId
              AND (:organizationId IS NULL OR organization_id = :organizationId)
              AND (:action IS NULL OR action = :action)
              AND (:actorUserId IS NULL OR actor_user_id = :actorUserId)
              AND (:from IS NULL OR created_at >= :from)
              AND (:to IS NULL OR created_at <= :to)
            ORDER BY created_at DESC
            LIMIT :size OFFSET :offset
            """)
    Flux<SystemAuditEntryEntity> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("organizationId") UUID organizationId,
            @Param("action") String action,
            @Param("actorUserId") UUID actorUserId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("size") int size,
            @Param("offset") long offset);

    @Query("""
            SELECT COUNT(*) FROM kernel.system_audit_entry
            WHERE tenant_id = :tenantId
              AND (:organizationId IS NULL OR organization_id = :organizationId)
              AND (:action IS NULL OR action = :action)
              AND (:actorUserId IS NULL OR actor_user_id = :actorUserId)
              AND (:from IS NULL OR created_at >= :from)
              AND (:to IS NULL OR created_at <= :to)
            """)
    Mono<Long> countFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("organizationId") UUID organizationId,
            @Param("action") String action,
            @Param("actorUserId") UUID actorUserId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
