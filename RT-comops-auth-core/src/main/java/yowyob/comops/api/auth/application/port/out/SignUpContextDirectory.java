package yowyob.comops.api.auth.application.port.out;

import java.util.UUID;
import reactor.core.publisher.Flux;

public interface SignUpContextDirectory {

    Flux<SignUpContext> findByOrganizationCode(String organizationCode);

    record SignUpContext(
            String contextId,
            UUID tenantId,
            UUID organizationId,
            String organizationCode,
            String organizationName,
            String organizationType) {
    }
}
