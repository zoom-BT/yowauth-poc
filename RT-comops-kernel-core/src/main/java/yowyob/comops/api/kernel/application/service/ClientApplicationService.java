package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import yowyob.comops.api.kernel.application.port.in.AuthenticateClientApplicationUseCase;
import yowyob.comops.api.kernel.application.port.in.ListClientApplicationsUseCase;
import yowyob.comops.api.kernel.application.port.in.ListClientApplicationPlansUseCase;
import yowyob.comops.api.kernel.application.port.in.ManageClientApplicationPlansUseCase;
import yowyob.comops.api.kernel.application.port.in.ClientApplicationPlanView;
import yowyob.comops.api.kernel.application.port.in.SaveClientApplicationPlanCommand;
import yowyob.comops.api.kernel.application.port.in.RegisterClientApplicationCommand;
import yowyob.comops.api.kernel.application.port.in.RegisterClientApplicationUseCase;
import yowyob.comops.api.kernel.application.port.in.RevokeClientApplicationUseCase;
import yowyob.comops.api.kernel.application.port.in.RotateClientApplicationSecretCommand;
import yowyob.comops.api.kernel.application.port.in.RotateClientApplicationSecretUseCase;
import yowyob.comops.api.kernel.application.port.in.UpdateClientApplicationCommand;
import yowyob.comops.api.kernel.application.port.in.UpdateClientApplicationUseCase;
import yowyob.comops.api.kernel.application.port.out.ClientApplicationPlanRepository;
import yowyob.comops.api.kernel.application.port.out.ClientApplicationRepository;
import yowyob.comops.api.kernel.config.SecurityRuntimeProperties;
import yowyob.comops.api.kernel.domain.ClientApplicationNotFoundException;
import yowyob.comops.api.kernel.domain.DuplicateClientApplicationIdException;
import yowyob.comops.api.kernel.domain.model.ClientApplication;
import yowyob.comops.api.kernel.domain.model.ClientApplicationPlan;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ClientApplicationService implements AuthenticateClientApplicationUseCase, ListClientApplicationsUseCase,
        RegisterClientApplicationUseCase, RotateClientApplicationSecretUseCase, RevokeClientApplicationUseCase,
        UpdateClientApplicationUseCase, ListClientApplicationPlansUseCase, ManageClientApplicationPlansUseCase {

    private static final SecureRandom SECRET_RANDOM = new SecureRandom();

    private final ClientApplicationRepository repository;
    private final ClientApplicationPlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServiceCatalog serviceCatalog;

    public ClientApplicationService(ClientApplicationRepository repository, ClientApplicationPlanRepository planRepository,
            PasswordEncoder passwordEncoder, ServiceCatalog serviceCatalog) {
        this.repository = repository;
        this.planRepository = planRepository;
        this.passwordEncoder = passwordEncoder;
        this.serviceCatalog = serviceCatalog;
    }

    @Override
    public Mono<ClientApplication> authenticate(String clientId, String clientSecret) {
        String normalizedClientId = normalizeClientId(clientId);
        String normalizedSecret = requireSecret(clientSecret);
        return repository.findByClientId(normalizedClientId)
                .filter(ClientApplication::isActive)
                .filter(clientApplication -> passwordEncoder.matches(normalizedSecret, clientApplication.secretHash()))
                .flatMap(clientApplication -> repository.save(clientApplication.markAuthenticated()));
    }

    @Override
    public Flux<ClientApplication> list() {
        return repository.findAll()
                .sort(Comparator.comparing(ClientApplication::clientId));
    }

    @Override
    public Mono<ProvisionedClientApplication> register(RegisterClientApplicationCommand command) {
        String normalizedClientId = normalizeClientId(command.clientId());
        String rawSecret = resolveSecret(command.clientSecret());
        return repository.existsByClientId(normalizedClientId)
                .flatMap(exists -> exists
                        ? Mono.error(new DuplicateClientApplicationIdException(normalizedClientId))
                        : resolveAllowedServices(command.planCode(), command.allowedServices())
                                .flatMap(allowedServices -> repository.save(ClientApplication.register(normalizedClientId,
                                        command.name(), command.description(), passwordEncoder.encode(rawSecret),
                                        allowedServices, command.systemManaged())))
                                .map(clientApplication -> new ProvisionedClientApplication(clientApplication, rawSecret)));
    }

    @Override
    public Mono<ClientApplication> update(UpdateClientApplicationCommand command) {
        return repository.findById(command.clientApplicationId())
                .switchIfEmpty(Mono.error(new ClientApplicationNotFoundException(command.clientApplicationId())))
                .flatMap(existing -> resolveAllowedServices(command.planCode(), command.allowedServices())
                        .flatMap(allowedServices -> repository.save(existing.updateDefinition(
                                command.name(),
                                command.description(),
                                allowedServices,
                                existing.systemManaged()))));
    }

    @Override
    public Mono<ProvisionedClientApplication> rotateSecret(RotateClientApplicationSecretCommand command) {
        String rawSecret = resolveSecret(command.clientSecret());
        return repository.findById(command.clientApplicationId())
                .switchIfEmpty(Mono.error(new ClientApplicationNotFoundException(command.clientApplicationId())))
                .flatMap(existing -> repository.save(existing.rotateSecret(passwordEncoder.encode(rawSecret)))
                        .map(clientApplication -> new ProvisionedClientApplication(clientApplication, rawSecret)));
    }

    @Override
    public Mono<ClientApplication> revoke(UUID clientApplicationId) {
        return repository.findById(clientApplicationId)
                .switchIfEmpty(Mono.error(new ClientApplicationNotFoundException(clientApplicationId)))
                .flatMap(existing -> repository.save(existing.revoke()));
    }

    @Override
    public Flux<ClientApplicationPlanView> listClientApplicationPlans() {
        return planRepository.findAll()
                .collectList()
                .map(this::mergeClientApplicationPlans)
                .flatMapMany(Flux::fromIterable)
                .map(this::toPlanView);
    }

    @Override
    public Mono<ClientApplicationPlanView> createClientApplicationPlan(SaveClientApplicationPlanCommand command) {
        String code = ClientApplicationPlan.normalizeCode(command.code());
        if (defaultClientApplicationPlansByCode().containsKey(code)) {
            return Mono.error(new IllegalArgumentException("default client application plan " + code + " cannot be recreated"));
        }
        ClientApplicationPlan plan = ClientApplicationPlan.create(code, command.displayName(), command.description(),
                command.allowedServices());
        return planRepository.existsByCode(code)
                .flatMap(exists -> exists
                        ? Mono.error(new IllegalArgumentException("client application plan already exists: " + code))
                        : planRepository.save(plan).map(this::toPlanView));
    }

    @Override
    public Mono<ClientApplicationPlanView> updateClientApplicationPlan(String planCode,
            SaveClientApplicationPlanCommand command) {
        String code = ClientApplicationPlan.normalizeCode(planCode);
        if (defaultClientApplicationPlansByCode().containsKey(code)) {
            return Mono.error(new IllegalArgumentException("default client application plan " + code + " cannot be modified"));
        }
        return planRepository.findByCode(code)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("unknown client application plan: " + code)))
                .map(existing -> existing.update(command.displayName(), command.description(), command.allowedServices()))
                .flatMap(planRepository::save)
                .map(this::toPlanView);
    }

    @Override
    public Mono<Void> deleteClientApplicationPlan(String planCode) {
        String code = ClientApplicationPlan.normalizeCode(planCode);
        if (defaultClientApplicationPlansByCode().containsKey(code)) {
            return Mono.error(new IllegalArgumentException("default client application plan " + code + " cannot be deleted"));
        }
        return planRepository.existsByCode(code)
                .flatMap(exists -> exists
                        ? planRepository.deleteByCode(code)
                        : Mono.error(new IllegalArgumentException("unknown client application plan: " + code)));
    }

    public Mono<ClientApplication> ensureBootstrapClient(SecurityRuntimeProperties.BootstrapClientProperties bootstrap) {
        if (bootstrap == null || !bootstrap.isEnabled()) {
            return Mono.empty();
        }
        String normalizedClientId = normalizeClientId(bootstrap.getClientId());
        String normalizedSecret = requireSecret(bootstrap.getSecret());
        String encodedSecret = passwordEncoder.encode(normalizedSecret);
        return repository.findByClientId(normalizedClientId)
                .flatMap(existing -> {
                    ClientApplication updated = existing.updateDefinition(
                            bootstrap.getName(),
                            bootstrap.getDescription(),
                            normalizeAllowedServices(bootstrap.getAllowedServices()),
                            true);
                    if (!existing.isActive()) {
                        updated = updated.activate();
                    }
                    if (!passwordEncoder.matches(normalizedSecret, existing.secretHash())) {
                        updated = updated.rotateSecret(encodedSecret);
                    }
                    return repository.save(updated);
                })
                .switchIfEmpty(repository.save(ClientApplication.register(normalizedClientId, bootstrap.getName(),
                        bootstrap.getDescription(), encodedSecret,
                        normalizeAllowedServices(bootstrap.getAllowedServices()), true)));
    }

    private Mono<Set<String>> resolveAllowedServices(String planCode, java.util.List<String> requestedServices) {
        if (requestedServices != null && !requestedServices.isEmpty()) {
            return Mono.just(normalizeAllowedServices(requestedServices));
        }
        if (planCode == null || planCode.isBlank()) {
            return Mono.just(normalizeAllowedServices(null));
        }
        return resolveClientApplicationPlan(planCode)
                .map(plan -> normalizeAllowedServices(plan.allowedServices()));
    }

    private Mono<ClientApplicationPlan> resolveClientApplicationPlan(String rawPlanCode) {
        String code = ClientApplicationPlan.normalizeCode(rawPlanCode);
        ClientApplicationPlan defaultPlan = defaultClientApplicationPlansByCode().get(code);
        if (defaultPlan != null) {
            return Mono.just(defaultPlan);
        }
        return planRepository.findByCode(code)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("unknown client application plan: " + rawPlanCode)));
    }

    private List<ClientApplicationPlan> mergeClientApplicationPlans(List<ClientApplicationPlan> customPlans) {
        Map<String, ClientApplicationPlan> merged = new LinkedHashMap<>(defaultClientApplicationPlansByCode());
        customPlans.forEach(plan -> merged.put(plan.code(), plan));
        return merged.values().stream()
                .sorted(Comparator.comparing(ClientApplicationPlan::systemDefault).reversed()
                        .thenComparing(ClientApplicationPlan::code))
                .toList();
    }

    private Map<String, ClientApplicationPlan> defaultClientApplicationPlansByCode() {
        Map<String, ClientApplicationPlan> defaults = new LinkedHashMap<>();
        putDefault(defaults, "PLATFORM_ADMIN_BACKEND", "Platform Admin Backend",
                "Administrative backend with all platform services.", PlatformServiceCode.catalog().stream().map(PlatformServiceCode::code).toList());
        putDefault(defaults, "CASHIER_BACKEND", "Cashier Backend",
                "External cashier-service allowed services.", List.of("ORGANIZATION", "SETTINGS", "COMMERCIAL",
                        "SALES", "BILLING", "ACCOUNTING", "TREASURY", "CASHIER", "NOTIFICATION"));
        putDefault(defaults, "ACCOUNTING_BACKEND", "Accounting Backend",
                "Accounting and settlement integration services.", List.of("ORGANIZATION", "SETTINGS", "COMMERCIAL",
                        "ACCOUNTING", "BANKING", "TREASURY", "BILLING", "NOTIFICATION"));
        putDefault(defaults, "HRM_BACKEND", "HRM Backend",
                "HRM and notification integration services.", List.of("ORGANIZATION", "SETTINGS", "HRM", "NOTIFICATION"));
        putDefault(defaults, "NOTIFICATION_WORKER", "Notification Worker",
                "Worker that consumes notification events and sends messages.", List.of("ORGANIZATION", "SETTINGS", "NOTIFICATION"));
        putDefault(defaults, "PUBLIC_FRONTEND_BFF", "Public Frontend BFF",
                "Frontend/BFF application with read-oriented business access.", List.of("ORGANIZATION", "SETTINGS", "COMMERCIAL", "PRODUCT", "SALES", "BILLING", "CASHIER"));
        return defaults;
    }

    private void putDefault(Map<String, ClientApplicationPlan> defaults, String code, String displayName,
            String description, List<String> allowedServices) {
        ClientApplicationPlan plan = ClientApplicationPlan.system(code, displayName, description, allowedServices);
        defaults.put(plan.code(), plan);
    }

    private ClientApplicationPlanView toPlanView(ClientApplicationPlan plan) {
        return new ClientApplicationPlanView(plan.code(), plan.displayName(), plan.description(), plan.allowedServices(),
                plan.systemDefault());
    }

    private String normalizeClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required");
        }
        return clientId.trim().toLowerCase(Locale.ROOT);
    }

    private String requireSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("clientSecret is required");
        }
        return secret.trim();
    }

    private String resolveSecret(String secret) {
        if (secret != null && !secret.isBlank()) {
            return secret.trim();
        }
        byte[] randomBytes = new byte[32];
        SECRET_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private Set<String> normalizeAllowedServices(java.util.List<String> requestedServices) {
        if (requestedServices == null || requestedServices.isEmpty()) {
            // Par défaut : tout le catalogue valide (natifs + services externes enregistrés actifs).
            return new LinkedHashSet<>(serviceCatalog.allCodes());
        }
        // Validation stricte via le catalogue (natif OU externe enregistré) -> rejette tout code inconnu.
        return new LinkedHashSet<>(serviceCatalog.validate(requestedServices));
    }
}
