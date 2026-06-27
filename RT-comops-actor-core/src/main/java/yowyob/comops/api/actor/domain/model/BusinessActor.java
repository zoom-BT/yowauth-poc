package yowyob.comops.api.actor.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface BusinessActor {

    UUID id();

    UUID tenantId();

    Instant createdAt();

    Instant updatedAt();

    UUID actorId();

    String code();

    boolean isIndividual();

    boolean isAvailable();

    boolean isVerified();

    boolean isActive();

    String type();

    String role();

    Set<String> qualifications();

    Set<String> paymentMethods();

    Set<UUID> addresses();

    String biography();

    Instant deletedAt();
}
