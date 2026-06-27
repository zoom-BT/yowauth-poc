package yowyob.comops.api.auth.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccess;
import yowyob.comops.api.auth.application.port.out.UserAccountRepository;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccessDirectory;
import yowyob.comops.api.auth.domain.model.UserAccount;

@Service
public class AuthSharedSessionService {

    private final UserAccountRepository userAccountRepository;
    private final UserOrganizationAccessDirectory userOrganizationAccessDirectory;
    private final AuthSsoSessionTokenService authSsoSessionTokenService;

    public AuthSharedSessionService(
            UserAccountRepository userAccountRepository,
            UserOrganizationAccessDirectory userOrganizationAccessDirectory,
            AuthSsoSessionTokenService authSsoSessionTokenService) {
        this.userAccountRepository = userAccountRepository;
        this.userOrganizationAccessDirectory = userOrganizationAccessDirectory;
        this.authSsoSessionTokenService = authSsoSessionTokenService;
    }

    public Mono<AuthSsoSessionTokenService.IssuedSsoSessionToken> issueForUser(UserAccount userAccount) {
        Objects.requireNonNull(userAccount, "userAccount is required");
        String principal = userAccount.email() != null && !userAccount.email().isBlank()
                ? userAccount.email()
                : userAccount.username();
        return userAccountRepository.findAllByPrincipal(principal)
                .map(account -> new AuthSsoSessionTokenService.SsoSessionContext(
                        UUID.randomUUID().toString(),
                        account.tenantId(),
                        account.id(),
                        account.actorId()))
                .collectList()
                .map(contexts -> contexts.isEmpty()
                        ? List.of(new AuthSsoSessionTokenService.SsoSessionContext(
                                UUID.randomUUID().toString(),
                                userAccount.tenantId(),
                                userAccount.id(),
                                userAccount.actorId()))
                        : List.copyOf(contexts))
                .map(contexts -> authSsoSessionTokenService.issue(principal, contexts));
    }

    public Mono<SharedSsoUserInfo> userInfo(String sharedSessionToken) {
        return Mono.justOrEmpty(authSsoSessionTokenService.verify(sharedSessionToken))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid or expired SSO session token.")))
                .flatMap(verified -> Flux.fromIterable(verified.contexts())
                        .flatMap(this::toContext)
                        .sort(Comparator.comparing(SharedSsoUserContext::tenantId).thenComparing(SharedSsoUserContext::contextId))
                        .collectList()
                        .map(contexts -> new SharedSsoUserInfo(
                                verified.subject(),
                                verified.sessionId(),
                                contexts)));
    }

    private Mono<SharedSsoUserContext> toContext(AuthSsoSessionTokenService.VerifiedSsoSessionContext context) {
        return userOrganizationAccessDirectory.listUserOrganizations(context.tenantId(), context.userId())
                .map(SharedSsoOrganizationAccess::from)
                .collectList()
                .map(organizations -> new SharedSsoUserContext(
                        context.contextId(),
                        context.tenantId(),
                        context.userId(),
                        context.actorId(),
                        organizations));
    }

    public record SharedSsoUserInfo(
            String subject,
            String sessionId,
            List<SharedSsoUserContext> contexts) {
    }

    public record SharedSsoUserContext(
            String contextId,
            UUID tenantId,
            UUID userId,
            UUID actorId,
            List<SharedSsoOrganizationAccess> organizations) {
    }

    public record SharedSsoOrganizationAccess(
            UUID organizationId,
            String organizationCode,
            String shortName,
            String longName,
            String displayName,
            String legalName,
            List<String> services) {

        public static SharedSsoOrganizationAccess from(UserOrganizationAccess access) {
            return new SharedSsoOrganizationAccess(access.organizationId(), access.organizationCode(),
                    access.shortName(), access.longName(), access.displayName(), access.legalName(), access.services());
        }
    }
}
