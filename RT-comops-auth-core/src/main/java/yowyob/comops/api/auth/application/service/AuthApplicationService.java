package yowyob.comops.api.auth.application.service;

import yowyob.comops.api.actor.application.port.in.CreateActorCommand;
import yowyob.comops.api.actor.application.port.in.CreateActorUseCase;
import yowyob.comops.api.auth.application.port.in.DiscoverSignUpContextsCommand;
import yowyob.comops.api.auth.application.port.in.DiscoverSignUpContextsResult;
import yowyob.comops.api.auth.application.port.in.DiscoverSignUpContextsUseCase;
import yowyob.comops.api.auth.application.port.in.GetCurrentUserProfileUseCase;
import yowyob.comops.api.auth.application.port.in.IdentifyAccountCommand;
import yowyob.comops.api.auth.application.port.in.IdentifyAccountResult;
import yowyob.comops.api.auth.application.port.in.IdentifyAccountUseCase;
import yowyob.comops.api.auth.application.port.in.LoginCommand;
import yowyob.comops.api.auth.application.port.in.LoginUseCase;
import yowyob.comops.api.auth.application.port.in.PublicSignUpCommand;
import yowyob.comops.api.auth.application.port.in.PublicSignUpUseCase;
import yowyob.comops.api.auth.application.port.in.RegisterUserCommand;
import yowyob.comops.api.auth.application.port.in.RegisterUserUseCase;
import yowyob.comops.api.auth.application.port.in.SelectableSignUpContext;
import yowyob.comops.api.auth.application.port.in.UpdateCurrentUserOnboardingUseCase;
import yowyob.comops.api.auth.application.port.in.UpdateCurrentUserPlanUseCase;
import yowyob.comops.api.auth.application.port.out.SignUpContextDirectory;
import yowyob.comops.api.auth.application.port.out.UserAccountRepository;
import yowyob.comops.api.auth.domain.DuplicateEmailException;
import yowyob.comops.api.auth.domain.DuplicateUsernameException;
import yowyob.comops.api.auth.domain.EmailNotVerifiedException;
import yowyob.comops.api.auth.domain.InvalidLoginCredentialsException;
import yowyob.comops.api.auth.domain.model.UserAccount;
import yowyob.comops.api.kernel.application.port.in.RecordSystemAuditUseCase;
import yowyob.comops.api.kernel.application.port.out.BusinessEventPublisher;
import yowyob.comops.api.kernel.application.port.out.TenantOwnerRoleProvisioner;
import yowyob.comops.api.kernel.application.service.ReactiveRequestContextHolder;
import yowyob.comops.api.kernel.domain.model.BusinessEvent;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthApplicationService implements RegisterUserUseCase, LoginUseCase,
        GetCurrentUserProfileUseCase, UpdateCurrentUserPlanUseCase, UpdateCurrentUserOnboardingUseCase,
        IdentifyAccountUseCase, PublicSignUpUseCase, DiscoverSignUpContextsUseCase {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(AuthApplicationService.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecordSystemAuditUseCase recordSystemAuditUseCase;
    private final BusinessEventPublisher businessEventPublisher;
    private final CreateActorUseCase createActorUseCase;
    private final SignUpContextDirectory signUpContextDirectory;
    private final AuthSignUpSelectionTokenService authSignUpSelectionTokenService;
    private final AuthPasswordResetTokenService authPasswordResetTokenService;
    private final AuthEmailVerificationTokenService authEmailVerificationTokenService;
    private final AuthEmailDeliveryService authEmailDeliveryService;
    private final AuthChallengeTokenService authChallengeTokenService;
    private final TenantOwnerRoleProvisioner tenantOwnerRoleProvisioner;

    public AuthApplicationService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
            RecordSystemAuditUseCase recordSystemAuditUseCase,
            BusinessEventPublisher businessEventPublisher,
            CreateActorUseCase createActorUseCase,
            SignUpContextDirectory signUpContextDirectory,
            AuthSignUpSelectionTokenService authSignUpSelectionTokenService,
            AuthPasswordResetTokenService authPasswordResetTokenService,
            AuthEmailVerificationTokenService authEmailVerificationTokenService,
            AuthEmailDeliveryService authEmailDeliveryService,
            AuthChallengeTokenService authChallengeTokenService,
            TenantOwnerRoleProvisioner tenantOwnerRoleProvisioner) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.recordSystemAuditUseCase = recordSystemAuditUseCase;
        this.businessEventPublisher = businessEventPublisher;
        this.createActorUseCase = createActorUseCase;
        this.signUpContextDirectory = signUpContextDirectory;
        this.authSignUpSelectionTokenService = authSignUpSelectionTokenService;
        this.authPasswordResetTokenService = authPasswordResetTokenService;
        this.authEmailVerificationTokenService = authEmailVerificationTokenService;
        this.authEmailDeliveryService = authEmailDeliveryService;
        this.authChallengeTokenService = authChallengeTokenService;
        this.tenantOwnerRoleProvisioner = tenantOwnerRoleProvisioner;
    }

    @Override
    public Mono<UserAccount> register(RegisterUserCommand command) {
        Objects.requireNonNull(command, "command is required");
        UserAccount userAccount = UserAccount.register(
                command.tenantId(),
                command.actorId(),
                command.username(),
                command.email(),
                passwordEncoder.encode(resolvePassword(command.password(), false)),
                command.authProvider())
                .updatePhoneNumber(command.phoneNumber());

        if (command.externalSubject() != null && !command.externalSubject().isBlank()) {
            userAccount = userAccount.linkExternalIdentity(command.authProvider(), command.externalSubject());
        }
        // Création administrative/programmatique = compte de confiance : email considéré vérifié
        // (contrairement au sign-up self-service qui exige une confirmation par email).
        userAccount = userAccount.markEmailVerified().markForInitialPersistence();

        return assertPrincipalAvailability(userAccount.tenantId(), userAccount.username(), userAccount.email(),
                userAccount.phoneNumber())
                .then(userAccountRepository.save(userAccount)
                        .flatMap(saved -> recordSystemAuditUseCase.record(saved.tenantId(), null, null,
                                        "USER_REGISTERED", "USER_ACCOUNT", saved.id().toString(),
                                        saved.username())
                                .then(publishAuthEvent("USER_REGISTERED", saved.tenantId(), null, saved.id(),
                                        Map.of("username", saved.username(), "email", saved.email(),
                                                "authProvider", saved.authProvider())))
                                .thenReturn(saved)));
    }

    @Override
    public Mono<UserAccount> login(LoginCommand command) {
        Objects.requireNonNull(command, "command is required");
        return userAccountRepository.findByPrincipal(command.tenantId(), command.principal())
                .filter(userAccount -> passwordEncoder.matches(command.password(), userAccount.passwordHash()))
                .switchIfEmpty(Mono.error(new InvalidLoginCredentialsException()))
                // Mode strict : un compte LOCAL non vérifié n'obtient aucune session.
                .flatMap(userAccount -> requiresEmailVerification(userAccount)
                        ? Mono.error(new EmailNotVerifiedException(userAccount.email()))
                        : Mono.just(userAccount))
                .flatMap(userAccount -> recordSystemAuditUseCase.record(userAccount.tenantId(), null, userAccount.id(),
                                "USER_LOGIN", "USER_ACCOUNT", userAccount.id().toString(), userAccount.username())
                        .then(publishAuthEvent("USER_LOGGED_IN", userAccount.tenantId(), null, userAccount.id(),
                                Map.of("username", userAccount.username(), "mfaEnabled", userAccount.mfaEnabled())))
                        .thenReturn(userAccount));
    }

    public Mono<AuthChallengeTokenService.IssuedOtpChallenge> issueLoginMfa(LoginCommand command) {
        return authenticateForMfa(command)
                .flatMap(this::issueLoginMfaForUser);
    }

    public Mono<AuthChallengeTokenService.IssuedOtpChallenge> issueLoginMfaForUser(UserAccount userAccount) {
        return Mono.just(userAccount)
                .map(account -> {
                    String channel = normalizeEnum(account.mfaChannel(), "EMAIL");
                    String recipient = mfaRecipient(account, channel);
                    AuthChallengeTokenService.IssuedOtpChallenge challenge = authChallengeTokenService.issueOtp(
                            "LOGIN_MFA",
                            channel,
                            recipient,
                            account.tenantId(),
                            account.id());
                    // Canal EMAIL : on envoie le code par e-mail. Si le SMTP l'a réellement
                    // expédié, on n'expose PAS le code dans la réponse HTTP (sinon le MFA ne
                    // protège rien). Sans SMTP configuré, on retombe sur le codePreview pour
                    // ne jamais verrouiller l'opérateur dehors.
                    if ("EMAIL".equals(channel)) {
                        AuthEmailDeliveryService.DeliveryResult delivery =
                                authEmailDeliveryService.deliverLoginMfaCode(
                                        recipient, challenge.codePreview(), challenge.expiresInSeconds());
                        if ("SMTP".equals(delivery.deliveryMode())) {
                            return new AuthChallengeTokenService.IssuedOtpChallenge(
                                    challenge.token(), null, challenge.expiresInSeconds());
                        }
                    }
                    return challenge;
                });
    }

    public Mono<UserAccount> confirmLoginMfa(String token, String code) {
        return Mono.justOrEmpty(authChallengeTokenService.verifyOtp(token, code, "LOGIN_MFA"))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid MFA challenge.")))
                .flatMap(verified -> userAccountRepository.findById(verified.tenantId(), verified.userId()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User account not found.")));
    }

    private Mono<UserAccount> authenticateForMfa(LoginCommand command) {
        Objects.requireNonNull(command, "command is required");
        return userAccountRepository.findByPrincipal(command.tenantId(), requirePrincipal(command.principal()))
                .filter(userAccount -> passwordEncoder.matches(command.password(), userAccount.passwordHash()))
                .switchIfEmpty(Mono.error(new InvalidLoginCredentialsException()))
                .filter(UserAccount::mfaEnabled)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("MFA is not enabled for this account.")));
    }

    @Override
    public Mono<UserAccount> getCurrentUserProfile() {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> userAccountRepository.findById(context.tenantId(), context.userId()));
    }

    @Override
    public Mono<UserAccount> updateCurrentUserPlan(String plan) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> userAccountRepository.findById(context.tenantId(), context.userId())
                        .flatMap(existing -> userAccountRepository.save(existing.updatePlan(plan))
                                .flatMap(saved -> recordSystemAuditUseCase.record(saved.tenantId(),
                                                context.organizationId(), context.userId(), "USER_PLAN_UPDATED",
                                                "USER_ACCOUNT", saved.id().toString(), saved.plan())
                                        .thenReturn(saved))));
    }

    @Override
    public Mono<UserAccount> updateCurrentUserOnboarding(int step, String status) {
        return ReactiveRequestContextHolder.getRequiredContext()
                .flatMap(context -> userAccountRepository.findById(context.tenantId(), context.userId())
                        .flatMap(existing -> userAccountRepository.save(existing.updateOnboarding(step, status))
                                .flatMap(saved -> recordSystemAuditUseCase.record(saved.tenantId(),
                                                context.organizationId(), context.userId(),
                                                "USER_ONBOARDING_UPDATED", "USER_ACCOUNT", saved.id().toString(),
                                                saved.onboardingStatus() + "@" + saved.onboardingStep())
                                        .thenReturn(saved))));
    }

    @Override
    public Mono<IdentifyAccountResult> identify(IdentifyAccountCommand command) {
        Objects.requireNonNull(command, "command is required");
        String principal = requirePrincipal(command.principal());
        return userAccountRepository.findAllByPrincipal(principal)
                .count()
                .map(count -> new IdentifyAccountResult(
                        principal,
                        count > 0,
                        count > 0 ? "SIGN_IN_PASSWORD" : "SIGN_UP",
                        count));
    }

    @Override
    public Mono<DiscoverSignUpContextsResult> discover(DiscoverSignUpContextsCommand command) {
        Objects.requireNonNull(command, "command is required");
        String organizationCode = requireOrganizationCode(command.organizationCode());
        return signUpContextDirectory.findByOrganizationCode(organizationCode)
                .collectList()
                .flatMap(contexts -> {
                    if (contexts.isEmpty()) {
                        return Mono.just(new DiscoverSignUpContextsResult(null, 0L, java.util.List.of()));
                    }
                    java.util.List<SelectableSignUpContext> selectable = contexts.stream()
                            .map(context -> new SelectableSignUpContext(
                                    context.contextId(),
                                    context.tenantId(),
                                    context.organizationId(),
                                    context.organizationCode(),
                                    context.organizationName(),
                                    context.organizationType()))
                            .toList();
                    AuthSignUpSelectionTokenService.IssuedSignUpSelectionToken issued =
                            authSignUpSelectionTokenService.issue(organizationCode, selectable);
                    return Mono.just(new DiscoverSignUpContextsResult(
                            issued.token(),
                            issued.expiresInSeconds(),
                            selectable));
                });
    }

    @Override
    public Mono<UserAccount> signUp(PublicSignUpCommand command) {
        Objects.requireNonNull(command, "command is required");
        String email = requirePrincipal(command.email());
        String username = resolveUsername(command.username(), email);
        if (command.captchaVerificationToken() != null && !command.captchaVerificationToken().isBlank()
                && !authChallengeTokenService.verifyCaptchaVerification(command.captchaVerificationToken())) {
            return Mono.error(new IllegalArgumentException("Invalid captcha verification token."));
        }
        String authProvider = resolveAuthProvider(command.socialProvider());
        String passwordHash = passwordEncoder.encode(resolvePassword(command.password(), !"LOCAL".equals(authProvider)));
        String onboardingPayload = command.onboardingPayload();
        // A self-service sign-up that provides neither an explicit tenantId nor a selection
        // token is creating its own brand-new tenant. The server generates the tenantId in
        // resolveTenantId(...) and the creator must be granted the owner role for that tenant.
        boolean createsNewTenant = command.tenantId() == null
                && (command.signUpSelectionToken() == null || command.signUpSelectionToken().isBlank());
        return resolveTenantId(command)
                .flatMap(tenantId -> assertPrincipalAvailability(tenantId, username, email, command.phoneNumber())
                .then(createActorUseCase.createActor(new CreateActorCommand(
                        tenantId,
                        command.firstName(),
                        command.lastName(),
                        null,
                        email,
                        null,
                        null,
                        null,
                        null,
                        null)))
                .flatMap(actor -> userAccountRepository.save(UserAccount.register(
                                tenantId,
                                actor.id(),
                                username,
                                email,
                                passwordHash,
                                authProvider)
                                .updatePhoneNumber(command.phoneNumber())
                                .linkExternalIdentity(authProvider, command.externalSubject())
                                .updateIdentityProfile(command.accountType(), command.businessType(),
                                        onboardingPayload, initialOnboardingStep(command.accountType()),
                                        initialOnboardingStatus(command.accountType()))
                                .markForInitialPersistence())
                        .flatMap(saved -> recordSystemAuditUseCase.record(saved.tenantId(), null, saved.id(),
                                        "USER_SIGNED_UP", "USER_ACCOUNT", saved.id().toString(), saved.email())
                                .then(recordSystemAuditUseCase.record(saved.tenantId(), null, saved.id(),
                                        "USER_REGISTERED", "USER_ACCOUNT", saved.id().toString(), saved.username()))
                                .then(publishAuthEvent("USER_REGISTERED", saved.tenantId(), null, saved.id(),
                                        Map.of("username", saved.username(), "email", saved.email(),
                                                "authProvider", saved.authProvider(), "source", "PUBLIC_SIGNUP")))
                                .then(createsNewTenant
                                        ? tenantOwnerRoleProvisioner.provisionOwner(saved.tenantId(), saved.id())
                                        : Mono.empty())
                                // Envoi automatique de l'email de vérification à l'inscription LOCAL.
                                // Le /sign-up ne le faisait pas (seul /email-verification/request l'envoyait),
                                // donc les nouveaux inscrits ne recevaient aucun mail. Fail-open.
                                .then("LOCAL".equals(authProvider)
                                        ? deliverSignUpEmailVerification(saved)
                                        : Mono.empty())
                                .thenReturn(saved))));
    }

    /** Mode strict : un compte LOCAL dont l'email n'est pas vérifié ne peut pas obtenir de session. */
    private boolean requiresEmailVerification(UserAccount userAccount) {
        return "LOCAL".equals(userAccount.authProvider())
                && userAccount.email() != null && !userAccount.email().isBlank()
                && !userAccount.emailVerified();
    }

    /** Émet et envoie l'email de vérification pour un compte fraîchement inscrit (fail-open). */
    private Mono<Void> deliverSignUpEmailVerification(UserAccount userAccount) {
        if (userAccount.email() == null || userAccount.email().isBlank() || userAccount.emailVerified()) {
            return Mono.empty();
        }
        return recordSystemAuditUseCase.record(userAccount.tenantId(), null, userAccount.id(),
                        "USER_EMAIL_VERIFICATION_REQUESTED", "USER_ACCOUNT",
                        userAccount.id().toString(), userAccount.email())
                .then(Mono.fromCallable(() -> {
                    AuthEmailVerificationTokenService.IssuedEmailVerificationToken issued =
                            authEmailVerificationTokenService.issue(userAccount);
                    return authEmailDeliveryService.deliverEmailVerification(
                            userAccount.email(), issued.token(), issued.expiresInSeconds());
                }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()))
                .then()
                .onErrorResume(error -> {
                    log.warn("Sign-up email verification delivery failed for user {}: {}",
                            userAccount.id(), error.toString());
                    return Mono.empty();
                });
    }

    public AuthChallengeTokenService.IssuedCaptchaChallenge issueCaptcha() {
        return authChallengeTokenService.issueCaptcha();
    }

    public Mono<AuthChallengeTokenService.IssuedCaptchaVerification> verifyCaptcha(String captchaToken, String answer) {
        return Mono.justOrEmpty(authChallengeTokenService.verifyCaptcha(captchaToken, answer))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid captcha answer.")));
    }

    public AuthChallengeTokenService.IssuedOtpChallenge issueOtp(String channel, String recipient, String purpose) {
        String normalizedPurpose = purpose == null || purpose.isBlank() ? "GENERIC" : purpose;
        return authChallengeTokenService.issueOtp(normalizedPurpose, channel, recipient, null, null);
    }

    public Mono<AuthChallengeTokenService.VerifiedOtpChallenge> verifyOtp(String token, String code, String purpose) {
        String normalizedPurpose = purpose == null || purpose.isBlank() ? "GENERIC" : purpose;
        return Mono.justOrEmpty(authChallengeTokenService.verifyOtp(token, code, normalizedPurpose))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid OTP challenge.")));
    }

    public Mono<AuthChallengeTokenService.IssuedOtpChallenge> issueCurrentUserMfaEnable(String channel) {
        return getCurrentUserProfile()
                .map(userAccount -> authChallengeTokenService.issueOtp(
                        "MFA_ENABLE",
                        channel,
                        mfaRecipient(userAccount, channel),
                        userAccount.tenantId(),
                        userAccount.id()));
    }

    public Mono<UserAccount> confirmCurrentUserMfaEnable(String token, String code) {
        return Mono.justOrEmpty(authChallengeTokenService.verifyOtp(token, code, "MFA_ENABLE"))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid MFA enable challenge.")))
                .flatMap(verified -> userAccountRepository.findById(verified.tenantId(), verified.userId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("User account not found.")))
                        .flatMap(userAccount -> userAccountRepository.save(userAccount.enableMfa(verified.channel()))
                                .flatMap(saved -> publishAuthEvent("USER_MFA_ENABLED", saved.tenantId(), null,
                                        saved.id(), Map.of("channel", saved.mfaChannel() == null ? "" : saved.mfaChannel()))
                                        .thenReturn(saved))));
    }

    public Mono<UserAccount> disableCurrentUserMfa() {
        return getCurrentUserProfile()
                .flatMap(userAccount -> userAccountRepository.save(userAccount.disableMfa()));
    }

    public Mono<AuthChallengeTokenService.IssuedOtpChallenge> issueCurrentUserPhoneVerification(String phoneNumber,
            String channel) {
        return getCurrentUserProfile()
                .flatMap(userAccount -> userAccountRepository.save(userAccount.updatePhoneNumber(phoneNumber)))
                .map(userAccount -> authChallengeTokenService.issueOtp(
                        "PHONE_VERIFICATION",
                        channel,
                        userAccount.phoneNumber(),
                        userAccount.tenantId(),
                        userAccount.id()));
    }

    public Mono<UserAccount> confirmCurrentUserPhoneVerification(String token, String code) {
        return Mono.justOrEmpty(authChallengeTokenService.verifyOtp(token, code, "PHONE_VERIFICATION"))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid phone verification challenge.")))
                .flatMap(verified -> userAccountRepository.findById(verified.tenantId(), verified.userId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("User account not found.")))
                        .flatMap(userAccount -> userAccountRepository.save(userAccount.markPhoneVerified())));
    }

    public Mono<UserAccount> updateCurrentIdentityOnboarding(String accountType, String businessType,
            String onboardingPayload, int step, String status) {
        return getCurrentUserProfile()
                .flatMap(userAccount -> userAccountRepository.save(userAccount.updateIdentityProfile(
                        accountType,
                        businessType,
                        onboardingPayload,
                        step,
                        status)));
    }

    public Mono<ForgotPasswordPayload> forgotPassword(String rawPrincipal) {
        String principal = requirePrincipal(rawPrincipal);
        return userAccountRepository.findAllByPrincipal(principal)
                .map(userAccount -> new AuthPasswordResetTokenService.PasswordResetContext(
                        userAccount.id().toString(),
                        userAccount.tenantId(),
                        userAccount.id(),
                        userAccount.actorId(),
                        userAccount.username(),
                        userAccount.email()))
                .collectList()
                .map(contexts -> {
                    if (contexts.isEmpty()) {
                        return new ForgotPasswordPayload(principal, 0L, null, 0L, java.util.List.of());
                    }
                    AuthPasswordResetTokenService.IssuedPasswordResetSelectionToken selection =
                            authPasswordResetTokenService.issueSelection(principal, contexts);
                    return new ForgotPasswordPayload(
                            principal,
                            contexts.size(),
                            selection.token(),
                            selection.expiresInSeconds(),
                            contexts);
                });
    }

    public Mono<AuthEmailDeliveryService.DeliveryResult> issuePasswordReset(
            String selectionToken,
            String contextId) {
        if (selectionToken == null || selectionToken.isBlank() || contextId == null || contextId.isBlank()) {
            return Mono.error(new IllegalArgumentException("selectionToken and contextId are required"));
        }
        return Mono.justOrEmpty(authPasswordResetTokenService.verifySelection(selectionToken))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid password reset selection token.")))
                .flatMapMany(token -> reactor.core.publisher.Flux.fromIterable(token.contexts()))
                .filter(context -> context.contextId().equals(contextId))
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Unknown password reset context.")))
                .flatMap(context -> userAccountRepository.findById(context.tenantId(), context.userId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("User account not found.")))
                        .flatMap(userAccount -> {
                            AuthPasswordResetTokenService.IssuedPasswordResetToken issued =
                                    authPasswordResetTokenService.issueReset(userAccount);
                            return recordSystemAuditUseCase.record(userAccount.tenantId(), null, userAccount.id(),
                                            "USER_PASSWORD_RESET_REQUESTED", "USER_ACCOUNT",
                                            userAccount.id().toString(), userAccount.email())
                                    .thenReturn(authEmailDeliveryService.deliverPasswordReset(
                                            userAccount.email(),
                                            issued.token(),
                                            issued.expiresInSeconds()));
                        }));
    }

    public Mono<UserAccount> resetPassword(String resetToken, String newPassword) {
        if (resetToken == null || resetToken.isBlank() || newPassword == null || newPassword.isBlank()) {
            return Mono.error(new IllegalArgumentException("resetToken and newPassword are required"));
        }
        return Mono.justOrEmpty(authPasswordResetTokenService.verifyReset(resetToken))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid password reset token.")))
                .flatMap(token -> userAccountRepository.findById(token.tenantId(), token.userId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("User account not found.")))
                        .flatMap(userAccount -> userAccountRepository.save(
                                        userAccount.updatePassword(passwordEncoder.encode(resolvePassword(newPassword, false))))
                                .flatMap(saved -> recordSystemAuditUseCase.record(saved.tenantId(), null, saved.id(),
                                                "USER_PASSWORD_RESET_COMPLETED", "USER_ACCOUNT",
                                                saved.id().toString(), saved.email())
                                        .then(publishAuthEvent("USER_PASSWORD_CHANGED", saved.tenantId(), null,
                                                saved.id(), Map.of("username", saved.username(), "method", "RESET")))
                                        .thenReturn(saved))));
    }

    public Mono<AuthEmailDeliveryService.DeliveryResult> issueCurrentEmailVerification() {
        return getCurrentUserProfile()
                .flatMap(userAccount -> {
                    if (userAccount.emailVerified()) {
                        return Mono.error(new IllegalArgumentException("Email is already verified."));
                    }
                    AuthEmailVerificationTokenService.IssuedEmailVerificationToken issued =
                            authEmailVerificationTokenService.issue(userAccount);
                    return recordSystemAuditUseCase.record(userAccount.tenantId(), null, userAccount.id(),
                                    "USER_EMAIL_VERIFICATION_REQUESTED", "USER_ACCOUNT",
                                    userAccount.id().toString(), userAccount.email())
                            .thenReturn(authEmailDeliveryService.deliverEmailVerification(
                                    userAccount.email(),
                                    issued.token(),
                                    issued.expiresInSeconds()));
                });
    }

    /**
     * Renvoi de l'email de vérification par PRINCIPAL, pour un utilisateur NON connecté (mode strict :
     * sa connexion est refusée tant qu'il n'a pas vérifié, donc il n'a pas de session). Réponse
     * générique si le compte est introuvable ou déjà vérifié (anti-énumération).
     */
    public Mono<AuthEmailDeliveryService.DeliveryResult> requestEmailVerificationForPrincipal(
            UUID tenantId, String principal) {
        if (principal == null || principal.isBlank()) {
            return Mono.error(new IllegalArgumentException("principal is required"));
        }
        return userAccountRepository.findByPrincipal(tenantId, principal)
                .filter(this::requiresEmailVerification)
                .flatMap(userAccount -> {
                    AuthEmailVerificationTokenService.IssuedEmailVerificationToken issued =
                            authEmailVerificationTokenService.issue(userAccount);
                    return recordSystemAuditUseCase.record(userAccount.tenantId(), null, userAccount.id(),
                                    "USER_EMAIL_VERIFICATION_REQUESTED", "USER_ACCOUNT",
                                    userAccount.id().toString(), userAccount.email())
                            .thenReturn(authEmailDeliveryService.deliverEmailVerification(
                                    userAccount.email(), issued.token(), issued.expiresInSeconds()));
                })
                // Compte introuvable ou déjà vérifié : on ne révèle rien.
                .switchIfEmpty(Mono.fromSupplier(() ->
                        AuthEmailDeliveryService.DeliveryResult.smtp(0L)));
    }

    public Mono<UserAccount> confirmEmailVerification(String verificationToken) {
        if (verificationToken == null || verificationToken.isBlank()) {
            return Mono.error(new IllegalArgumentException("verificationToken is required"));
        }
        return Mono.justOrEmpty(authEmailVerificationTokenService.verify(verificationToken))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid email verification token.")))
                .flatMap(token -> userAccountRepository.findById(token.tenantId(), token.userId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("User account not found.")))
                        .flatMap(userAccount -> userAccountRepository.save(userAccount.markEmailVerified())
                                .flatMap(saved -> recordSystemAuditUseCase.record(saved.tenantId(), null, saved.id(),
                                                "USER_EMAIL_VERIFIED", "USER_ACCOUNT", saved.id().toString(),
                                                saved.email())
                                        .thenReturn(saved))));
    }

    private String resolvePassword(String rawPassword, boolean allowGenerated) {
        if (rawPassword == null || rawPassword.isBlank()) {
            if (!allowGenerated) {
                throw new IllegalArgumentException("password is required");
            }
            return "bootstrap-" + UUID.randomUUID();
        }
        String password = rawPassword.trim();
        requireStrongPassword(password);
        return password;
    }

    private Mono<Void> assertPrincipalAvailability(UUID tenantId, String username, String email, String phoneNumber) {
        Mono<Boolean> usernameTaken = userAccountRepository.findByPrincipal(tenantId, username).hasElement();
        Mono<Boolean> emailTaken = userAccountRepository.findByPrincipal(tenantId, email).hasElement();
        Mono<Boolean> phoneTaken = phoneNumber == null || phoneNumber.isBlank()
                ? Mono.just(false)
                : userAccountRepository.findByPrincipal(tenantId, phoneNumber).hasElement();
        return Mono.zip(usernameTaken, emailTaken, phoneTaken)
                .flatMap(tuple -> {
                    if (tuple.getT1()) {
                        return Mono.error(new DuplicateUsernameException(username));
                    }
                    if (tuple.getT2()) {
                        return Mono.error(new DuplicateEmailException(email));
                    }
                    if (tuple.getT3()) {
                        return Mono.error(new IllegalArgumentException("Phone number is already used."));
                    }
                    return Mono.<Void>empty();
                });
    }

    private String resolveUsername(String username, String email) {
        if (username == null || username.isBlank()) {
            return email;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String requirePrincipal(String principal) {
        if (principal == null || principal.isBlank()) {
            throw new IllegalArgumentException("principal is required");
        }
        return principal.trim().toLowerCase(Locale.ROOT);
    }

    private void requireStrongPassword(String password) {
        if (password.length() < 10
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new IllegalArgumentException(
                    "password must contain at least 10 characters, upper and lower case letters, a digit and a symbol");
        }
    }

    private String resolveAuthProvider(String socialProvider) {
        if (socialProvider == null || socialProvider.isBlank()) {
            return "LOCAL";
        }
        return socialProvider.trim().toUpperCase(Locale.ROOT);
    }

    private int initialOnboardingStep(String accountType) {
        return "BUSINESS".equals(normalizeEnum(accountType, "PROSPECT")) ? 1 : 0;
    }

    private String initialOnboardingStatus(String accountType) {
        return "BUSINESS".equals(normalizeEnum(accountType, "PROSPECT")) ? "IN_PROGRESS" : "NOT_STARTED";
    }

    private String normalizeEnum(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private String mfaRecipient(UserAccount userAccount, String channel) {
        String normalized = normalizeEnum(channel, "EMAIL");
        if (("SMS".equals(normalized) || "WHATSAPP".equals(normalized)) && userAccount.phoneNumber() != null) {
            return userAccount.phoneNumber();
        }
        return userAccount.email();
    }

    private String requireOrganizationCode(String organizationCode) {
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new IllegalArgumentException("organizationCode is required");
        }
        return organizationCode.trim().toUpperCase(Locale.ROOT);
    }

    private Mono<UUID> resolveTenantId(PublicSignUpCommand command) {
        if (command.tenantId() != null) {
            return Mono.just(command.tenantId());
        }
        boolean hasSelectionToken = command.signUpSelectionToken() != null
                && !command.signUpSelectionToken().isBlank();
        if (!hasSelectionToken) {
            // Aucun tenantId ni token de sélection : l'utilisateur crée sa propre organisation.
            // Le serveur génère le tenant pour garantir son unicité (ne pas faire confiance au client).
            return Mono.just(UUID.randomUUID());
        }
        if (command.contextId() == null || command.contextId().isBlank()) {
            return Mono.error(new IllegalArgumentException(
                    "contextId is required when signUpSelectionToken is provided"));
        }
        return Mono.justOrEmpty(authSignUpSelectionTokenService.verify(command.signUpSelectionToken()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid sign-up selection token.")))
                .flatMapMany(token -> reactor.core.publisher.Flux.fromIterable(token.contexts()))
                .filter(context -> context.contextId().equals(command.contextId()))
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Unknown sign-up context.")))
                .map(AuthSignUpSelectionTokenService.VerifiedSelectableSignUpContext::tenantId);
    }

    private Mono<Void> publishAuthEvent(String eventType, UUID tenantId, UUID organizationId, UUID userId,
            Map<String, Object> extraPayload) {
        if (tenantId == null || userId == null) {
            return Mono.empty();
        }
        Map<String, Object> payload = new LinkedHashMap<>(extraPayload);
        payload.put("userId", userId);
        BusinessEvent event = BusinessEvent.now(tenantId, organizationId, eventType, "USER_ACCOUNT", userId, payload);
        return businessEventPublisher.publish(event).onErrorResume(ex -> Mono.empty());
    }

    public record ForgotPasswordPayload(
            String principal,
            long matchingAccountCount,
            String selectionToken,
            long selectionTokenExpiresInSeconds,
            java.util.List<AuthPasswordResetTokenService.PasswordResetContext> contexts) {
    }
}
