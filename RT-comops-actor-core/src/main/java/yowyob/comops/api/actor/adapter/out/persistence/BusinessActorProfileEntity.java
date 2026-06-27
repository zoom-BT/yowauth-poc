package yowyob.comops.api.actor.adapter.out.persistence;

import yowyob.comops.api.common.adapter.out.persistence.PersistableEntity;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "actor", name = "business_actor_profile")
public record BusinessActorProfileEntity(
        @Id UUID id,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt,
        UUID actorId,
        String governanceStatus,
        UUID governedByUserId,
        Instant governedAt,
        String governanceReason,
        String code,
        boolean isIndividual,
        boolean isAvailable,
        boolean isVerified,
        boolean isActive,
        String type,
        String role,
        Set<String> qualifications,
        Set<String> paymentMethods,
        Set<UUID> addresses,
        String biography,
        Instant deletedAt,
        String name,
        String businessId,
        String niu,
        String tradeRegistryNumber,
        String website,
        String contactPhone,
        String privateAddress,
        String businessAddress,
        String businessProfile) implements PersistableEntity {
}
