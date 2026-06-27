package yowyob.comops.api.auth.application.port.out;

import java.util.List;
import java.util.UUID;

public record UserOrganizationAccess(
        UUID organizationId,
        String organizationCode,
        String shortName,
        String longName,
        List<String> services) {

    public String displayName() {
        return shortName;
    }

    public String legalName() {
        return longName;
    }
}
