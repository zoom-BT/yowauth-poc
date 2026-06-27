package yowyob.comops.api.auth.adapter.in.web;

import yowyob.comops.api.auth.application.port.out.UserOrganizationAccessDirectory;
import yowyob.comops.api.auth.application.service.AuthSharedSessionService;
import yowyob.comops.api.auth.domain.model.UserAccount;
import java.time.Duration;
import java.util.Set;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthUserViewAssembler {

    private final UserOrganizationAccessDirectory userOrganizationAccessDirectory;
    private final AuthSharedSessionService authSharedSessionService;

    public AuthUserViewAssembler(
            UserOrganizationAccessDirectory userOrganizationAccessDirectory,
            AuthSharedSessionService authSharedSessionService) {
        this.userOrganizationAccessDirectory = userOrganizationAccessDirectory;
        this.authSharedSessionService = authSharedSessionService;
    }

    public Mono<LoginResponse> toLoginResponse(UserAccount userAccount, String accessToken, Duration accessTokenTtl,
            Set<String> authorities) {
        return Mono.zip(
                        organizations(userAccount),
                        authSharedSessionService.issueForUser(userAccount).map(SharedSsoSessionResponse::from))
                .map(tuple -> LoginResponse.from(
                        userAccount,
                        accessToken,
                        accessTokenTtl,
                        tuple.getT2(),
                        authorities,
                        tuple.getT1()));
    }

    public Mono<UserAccountResponse> toUserAccountResponse(UserAccount userAccount) {
        return organizations(userAccount)
                .map(organizations -> UserAccountResponse.from(userAccount, organizations));
    }

    private Mono<java.util.List<UserOrganizationAccessResponse>> organizations(UserAccount userAccount) {
        return userOrganizationAccessDirectory.listUserOrganizations(userAccount.tenantId(), userAccount.id())
                .map(UserOrganizationAccessResponse::from)
                .collectList();
    }
}
