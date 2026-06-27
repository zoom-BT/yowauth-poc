package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import yowyob.comops.api.kernel.application.port.out.ExternalServiceRepository;
import yowyob.comops.api.kernel.domain.model.ExternalServiceDefinition;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Catalogue des services de la plateforme = services NATIFS (enum {@link PlatformServiceCode}) UNION
 * services EXTERNES enregistrés dynamiquement (registre DB). Permet d'ajouter des services sans
 * toucher au code, tout en gardant une validation stricte (un code doit être natif OU enregistré)
 * pour éviter les abus. Les codes externes actifs sont gardés dans un cache mémoire (rafraîchi au
 * démarrage et à chaque register/deactivate) pour une validation synchrone.
 */
@Component
public class ServiceCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCatalog.class);

    private final ExternalServiceRepository repository;
    private final Set<String> registeredActiveCodes = ConcurrentHashMap.newKeySet();

    public ServiceCatalog(ExternalServiceRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        refresh().doOnError(e -> LOGGER.warn("Failed to load external service registry into cache.", e)).subscribe();
    }

    public Mono<Void> refresh() {
        return repository.findAll()
                .filter(ExternalServiceDefinition::active)
                .map(ExternalServiceDefinition::code)
                .collectList()
                .doOnNext(codes -> {
                    registeredActiveCodes.clear();
                    registeredActiveCodes.addAll(codes);
                })
                .then();
    }

    /** True si le code (natif ou externe actif) est connu du catalogue. */
    public boolean isKnown(String rawCode) {
        String canonical = PlatformServiceCode.resolveCanonical(rawCode);
        return isNativeCode(canonical) || registeredActiveCodes.contains(canonical);
    }

    /** Code canonique si connu, sinon exception (validation stricte anti-abus). */
    public String canonicalizeOrThrow(String rawCode) {
        String canonical = PlatformServiceCode.resolveCanonical(rawCode);
        if (isNativeCode(canonical) || registeredActiveCodes.contains(canonical)) {
            return canonical;
        }
        throw new IllegalArgumentException("unknown platform service: " + rawCode);
    }

    /** Valide + canonicalise une liste de codes (services demandés pour une ClientApplication). */
    public Set<String> validate(Collection<String> requestedCodes) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String code : requestedCodes) {
            result.add(canonicalizeOrThrow(code));
        }
        return result;
    }

    /** Tous les codes valides (natifs + externes actifs) — utile pour les valeurs par défaut. */
    public Set<String> allCodes() {
        LinkedHashSet<String> all = new LinkedHashSet<>(
                PlatformServiceCode.catalog().stream().map(PlatformServiceCode::code).toList());
        all.addAll(registeredActiveCodes);
        return all;
    }

    public Mono<ExternalServiceDefinition> register(ExternalServiceDefinition definition) {
        return repository.save(definition)
                .doOnNext(saved -> {
                    if (saved.active()) {
                        registeredActiveCodes.add(saved.code());
                    } else {
                        registeredActiveCodes.remove(saved.code());
                    }
                });
    }

    public Mono<Void> deactivate(String code) {
        String canonical = PlatformServiceCode.normalizeCode(code);
        return repository.deactivate(canonical).doOnSuccess(v -> registeredActiveCodes.remove(canonical));
    }

    public Flux<ExternalServiceDefinition> registeredServices() {
        return repository.findAll();
    }

    private boolean isNativeCode(String canonical) {
        return PlatformServiceCode.catalog().stream().anyMatch(s -> s.code().equals(canonical));
    }

    // ── Helpers transverses (natif enum OU service externe enregistré) ───────────────────────────
    // Permettent aux autres couches (ex. abonnements organisation) de raisonner uniformément sur
    // un code service sans dupliquer la dualité enum/externe.

    public boolean isNative(String rawCode) {
        return isNativeCode(PlatformServiceCode.resolveCanonical(rawCode));
    }

    /** Un service externe enregistré est toujours abonnable ; un natif l'est s'il n'est pas obligatoire. */
    public boolean isSubscribable(String rawCode) {
        String canonical = PlatformServiceCode.resolveCanonical(rawCode);
        if (isNativeCode(canonical)) {
            return PlatformServiceCode.from(canonical).subscribable();
        }
        return registeredActiveCodes.contains(canonical);
    }

    /** Seuls des services natifs peuvent être obligatoires ; un service externe ne l'est jamais. */
    public boolean isMandatory(String rawCode) {
        String canonical = PlatformServiceCode.resolveCanonical(rawCode);
        return isNativeCode(canonical) && PlatformServiceCode.from(canonical).mandatory();
    }

    /** Dépendances requises : celles de l'enum pour un natif, aucune pour un service externe. */
    public java.util.List<String> requiredDependencyCodes(String rawCode) {
        String canonical = PlatformServiceCode.resolveCanonical(rawCode);
        return isNativeCode(canonical) ? PlatformServiceCode.from(canonical).requiredDependencyCodes()
                : java.util.List.of();
    }
}
