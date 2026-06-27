package yowyob.comops.api.actor.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import yowyob.comops.api.actor.domain.model.BusinessActor;
import yowyob.comops.api.actor.domain.model.BusinessActorProfile;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusinessActorResponseTest {

    @Test
    void mapsCanonicalBusinessActorContractFromProfile() {
        BusinessActor actor = BusinessActorProfile.create(UUID.randomUUID(), UUID.randomUUID(), "SUP-01", false, true,
                true, true, "SUPPLIER", "OWNER", Set.of("ISO9001"), Set.of("CASH"), Set.of(), "Bio", "Savhel",
                "RC123", "NIU123", "TR123", "https://example.com", "+237600000000", "private", "business", "profile");

        BusinessActorResponse response = BusinessActorResponse.from(actor);

        assertThat(response.code()).isEqualTo("SUP_01");
        assertThat(response.type()).isEqualTo("SUPPLIER");
        assertThat(response.qualifications()).containsExactly("ISO9001");
        assertThat(response.name()).isEqualTo("Savhel");
    }
}
