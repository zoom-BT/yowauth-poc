package yowyob.comops.api.common.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PartyRef(PartyType partyType, UUID partyId) {

    public PartyRef {
        Objects.requireNonNull(partyType, "partyType is required");
        Objects.requireNonNull(partyId, "partyId is required");
    }
}
