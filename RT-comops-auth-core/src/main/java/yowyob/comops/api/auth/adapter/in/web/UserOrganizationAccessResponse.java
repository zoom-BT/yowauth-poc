package yowyob.comops.api.auth.adapter.in.web;

import java.util.List;
import java.util.UUID;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccess;
import yowyob.comops.api.auth.application.service.AuthSharedSessionService;

public record UserOrganizationAccessResponse(
        UUID organizationId,
        String organizationCode,
        String shortName,
        String longName,
        String displayName,
        String legalName,
        List<String> services) {

    public static UserOrganizationAccessResponse from(UserOrganizationAccess access) {
        return new UserOrganizationAccessResponse(access.organizationId(), access.organizationCode(),
                access.shortName(), access.longName(), access.displayName(), access.legalName(), access.services());
    }

    public static UserOrganizationAccessResponse from(AuthSharedSessionService.SharedSsoOrganizationAccess access) {
        return new UserOrganizationAccessResponse(access.organizationId(), access.organizationCode(),
                access.shortName(), access.longName(), access.displayName(), access.legalName(), access.services());
    }
}
