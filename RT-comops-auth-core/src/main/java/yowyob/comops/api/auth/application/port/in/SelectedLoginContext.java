package yowyob.comops.api.auth.application.port.in;

import yowyob.comops.api.auth.domain.model.UserAccount;
import java.util.UUID;

public record SelectedLoginContext(
        UserAccount userAccount,
        UUID organizationId) {
}
