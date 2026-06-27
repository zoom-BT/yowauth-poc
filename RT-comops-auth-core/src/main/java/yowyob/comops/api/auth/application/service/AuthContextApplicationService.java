package yowyob.comops.api.auth.application.service;

import yowyob.comops.api.auth.application.port.in.DiscoverLoginContextsCommand;
import yowyob.comops.api.auth.application.port.in.DiscoverLoginContextsResult;
import yowyob.comops.api.auth.application.port.in.DiscoverLoginContextsUseCase;
import yowyob.comops.api.auth.application.port.in.SelectLoginContextCommand;
import yowyob.comops.api.auth.application.port.in.SelectLoginContextUseCase;
import yowyob.comops.api.auth.application.port.in.SelectableLoginContext;
import yowyob.comops.api.auth.application.port.in.SelectedLoginContext;
import yowyob.comops.api.auth.application.port.out.UserAccountRepository;
import yowyob.comops.api.auth.application.port.out.UserOrganizationAccessDirectory;
import yowyob.comops.api.auth.domain.InvalidLoginCredentialsException;
import yowyob.comops.api.auth.domain.model.UserAccount;
import yowyob.comops.api.kernel.application.port.in.RecordSystemAuditUseCase;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthContextApplicationService implements DiscoverLoginContextsUseCase, SelectLoginContextUseCase {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserOrganizationAccessDirectory userOrganizationAccessDirectory;
    private final AuthContextSelectionTokenService authContextSelectionTokenService;
    private final RecordSystemAuditUseCase recordSystemAuditUseCase;

    public AuthContextApplicationService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            UserOrganizationAccessDirectory userOrganizationAccessDirectory,
            AuthContextSelectionTokenService authContextSelectionTokenService,
            RecordSystemAuditUseCase recordSystemAuditUseCase) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.userOrganizationAccessDirectory = userOrganizationAccessDirectory;
        this.authContextSelectionTokenService = authContextSelectionTokenService;
        this.recordSystemAuditUseCase = recordSystemAuditUseCase;
    }

    @Override
    public Mono<DiscoverLoginContextsResult> discover(DiscoverLoginContextsCommand command) {
        Objects.requireNonNull(command, "command is required");
        String principal = requireText(command.principal(), "principal");
        String password = requireText(command.password(), "password");
        return userAccountRepository.findAllByPrincipal(principal)
                .filter(userAccount -> passwordEncoder.matches(password, userAccount.passwordHash()))
                .flatMap(this::toSelectableContext)
                .collectList()
                .filter(contexts -> !contexts.isEmpty())
                .switchIfEmpty(Mono.error(new InvalidLoginCredentialsException()))
                .flatMap(contexts -> Mono.fromSupplier(() -> {
                    AuthContextSelectionTokenService.IssuedContextSelectionToken issuedToken =
                            authContextSelectionTokenService.issue(principal, contexts);
                    return new DiscoverLoginContextsResult(
                            issuedToken.token(),
                            issuedToken.expiresInSeconds(),
                            contexts);
                }));
    }

    @Override
    public Mono<SelectedLoginContext> select(SelectLoginContextCommand command) {
        Objects.requireNonNull(command, "command is required");
        String selectionToken = requireText(command.selectionToken(), "selectionToken");
        String contextId = requireText(command.contextId(), "contextId");

        return Mono.fromSupplier(() -> authContextSelectionTokenService.verify(selectionToken)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Invalid or expired context selection token.")))
                .map(verifiedToken -> verifiedToken.contexts().stream()
                        .filter(context -> context.contextId().equals(contextId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "The selected login context is not available.")))
                .flatMap(context -> userAccountRepository.findById(context.tenantId(), context.userId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                "The selected login context is no longer available.")))
                        .flatMap(userAccount -> validateOrganizationAccess(userAccount, command.organizationId())
                                .then(recordSystemAuditUseCase.record(userAccount.tenantId(), command.organizationId(),
                                                userAccount.id(), "USER_LOGIN", "USER_ACCOUNT",
                                                userAccount.id().toString(), userAccount.username())
                                        .thenReturn(new SelectedLoginContext(userAccount, command.organizationId())))));
    }

    private Mono<SelectableLoginContext> toSelectableContext(UserAccount userAccount) {
        return userOrganizationAccessDirectory.listUserOrganizations(userAccount.tenantId(), userAccount.id())
                .collectList()
                .map(organizations -> new SelectableLoginContext(
                        UUID.randomUUID().toString(),
                        userAccount.tenantId(),
                        userAccount.id(),
                        userAccount.actorId(),
                        List.copyOf(organizations)));
    }

    private Mono<Void> validateOrganizationAccess(UserAccount userAccount, UUID organizationId) {
        if (organizationId == null) {
            return Mono.empty();
        }
        return userOrganizationAccessDirectory.listUserOrganizations(userAccount.tenantId(), userAccount.id())
                .filter(access -> organizationId.equals(access.organizationId()))
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "The selected organization is not accessible in this context.")))
                .then();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
