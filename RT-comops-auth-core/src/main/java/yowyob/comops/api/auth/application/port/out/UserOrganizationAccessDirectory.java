package yowyob.comops.api.auth.application.port.out;

import java.util.UUID;
import reactor.core.publisher.Flux;

public interface UserOrganizationAccessDirectory {

    Flux<UserOrganizationAccess> listUserOrganizations(UUID tenantId, UUID userId);
}
