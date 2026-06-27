package yowyob.comops.api.kernel.domain.model;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    DEAD_LETTER
}
