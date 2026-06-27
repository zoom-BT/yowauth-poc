package yowyob.comops.api.actor.domain.model;

public enum BusinessActorGovernanceStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    SUSPENDED,
    BLOCKED;

    public static BusinessActorGovernanceStatus from(String value) {
        if (value == null || value.isBlank()) {
            return PENDING_REVIEW;
        }
        return BusinessActorGovernanceStatus.valueOf(value.trim().toUpperCase());
    }
}
