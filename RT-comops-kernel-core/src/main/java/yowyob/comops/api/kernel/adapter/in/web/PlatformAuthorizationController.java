package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.config.ApiKeyAuthenticationToken;
import yowyob.comops.api.kernel.config.TenantRequestQuotaProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Autorisation serveur-à-serveur pour les SERVICES EXTERNES (déployés hors kernel : accounting,
 * VerifID, etc.). Comme ces services sont appelés en direct, les filtres entitlement/quota du
 * kernel ne sont pas dans leur chemin : pour éviter les abus, chaque service externe appelle CET
 * endpoint (en transmettant les creds du client appelant) AVANT de traiter une requête.
 *
 * Il fait deux choses, avec le kernel comme source de vérité :
 *   1) ENTITLEMENT : le client appelant a-t-il le service demandé dans ses allowedServices ?
 *   2) QUOTA       : incrémente le MÊME compteur Redis que le gateway interne
 *      (clé tenant:client:service:bucket) -> un service externe ne peut pas dépasser le quota
 *      plateforme ni le contourner.
 *
 * Réponse : 200 {allowed:true, limit, remaining, windowSeconds} | 403 non habilité | 429 quota dépassé.
 */
@RestController
@RequestMapping("/api/client-applications/me")
public class PlatformAuthorizationController {

    private final ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider;
    private final ObjectProvider<TenantRequestQuotaProperties> quotaPropertiesProvider;
    private final ObjectProvider<yowyob.comops.api.kernel.application.port.out
            .OrganizationServiceRuntimeEntitlementDirectory> orgEntitlementProvider;

    public PlatformAuthorizationController(
            ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider,
            ObjectProvider<TenantRequestQuotaProperties> quotaPropertiesProvider,
            ObjectProvider<yowyob.comops.api.kernel.application.port.out
                    .OrganizationServiceRuntimeEntitlementDirectory> orgEntitlementProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.quotaPropertiesProvider = quotaPropertiesProvider;
        this.orgEntitlementProvider = orgEntitlementProvider;
    }

    @PostMapping("/authorize")
    @PreAuthorize("@businessAccessPolicy.isAuthenticatedClientApplication(authentication)")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> authorize(
            @RequestParam("service") String service,
            Authentication authentication) {
        ApiKeyAuthenticationToken token = (ApiKeyAuthenticationToken) authentication;
        String serviceCode = normalize(service);

        // 1) Entitlement
        if (!token.allowedServiceCodes().contains(serviceCode)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.failure("Client application is not allowed to access service " + serviceCode + ".",
                            "CLIENT_APPLICATION_SERVICE_NOT_ALLOWED")));
        }

        // 2) Abonnement ORGANISATION (si un contexte org est fourni) : la plateforme reste juge.
        //    Permet aux services externes d'imposer l'abonnement org, pas seulement l'entitlement client.
        var orgDirectory = orgEntitlementProvider.getIfAvailable();
        if (token.tenantId() != null && token.organizationId() != null && orgDirectory != null) {
            return orgDirectory.resolveRuntimeEntitlement(token.tenantId(), token.organizationId(), serviceCode)
                    .flatMap(ent -> {
                        if (ent.effective()) {
                            return enforceTenantQuota(token, serviceCode);
                        }
                        ResponseEntity<ApiResponse<Map<String, Object>>> denied =
                                ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                                        ApiResponse.<Map<String, Object>>failure(
                                                "Organization is not subscribed to service " + serviceCode + ".",
                                                "ORGANIZATION_SERVICE_NOT_SUBSCRIBED"));
                        return Mono.just(denied);
                    })
                    .switchIfEmpty(Mono.defer(() -> enforceTenantQuota(token, serviceCode)));
        }
        return enforceTenantQuota(token, serviceCode);
    }

    /** 3) Quota tenant×client×service partagé avec le gateway interne (mêmes buckets Redis). */
    private Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> enforceTenantQuota(
            ApiKeyAuthenticationToken token, String serviceCode) {
        TenantRequestQuotaProperties props = quotaPropertiesProvider.getIfAvailable();
        ReactiveStringRedisTemplate redis = redisTemplateProvider.getIfAvailable();

        // Quota désactivé ou Redis absent -> on autorise (entitlement déjà validé).
        if (props == null || !props.isEnabled() || redis == null
                || token.tenantId() == null || token.clientId() == null || token.clientId().isBlank()) {
            return Mono.just(ResponseEntity.ok(ApiResponse.success(
                    Map.of("allowed", true, "service", serviceCode, "quotaEnforced", false),
                    "Authorized.")));
        }

        long windowSeconds = Math.max(1L, props.getWindow().toSeconds());
        long bucket = Instant.now().getEpochSecond() / windowSeconds;
        // MÊME format de clé que TenantRequestQuotaWebFilter -> buckets partagés.
        String key = props.getKeyPrefix()
                + ":" + token.tenantId()
                + ":" + token.clientId().toLowerCase(Locale.ROOT)
                + ":" + serviceCode
                + ":" + bucket;
        Duration ttl = props.getWindow().plusSeconds(5);

        return redis.opsForValue().increment(key)
                .flatMap(count -> (count != null && count == 1L ? redis.expire(key, ttl) : Mono.just(Boolean.FALSE))
                        .thenReturn(count == null ? 0L : count))
                .onErrorResume(error -> props.isFailOpen() ? Mono.just(-1L) : Mono.just(Long.MAX_VALUE))
                .map(count -> {
                    if (count == -1L) {
                        return ResponseEntity.ok(ApiResponse.success(
                                Map.of("allowed", true, "service", serviceCode, "quotaEnforced", false),
                                "Authorized (quota fail-open)."));
                    }
                    long remaining = Math.max(0L, props.getLimit() - count);
                    Map<String, Object> body = Map.of(
                            "service", serviceCode,
                            "limit", props.getLimit(),
                            "remaining", remaining,
                            "windowSeconds", windowSeconds,
                            "quotaEnforced", true,
                            "allowed", count <= props.getLimit());
                    if (count > props.getLimit()) {
                        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .header("Retry-After", Long.toString(windowSeconds))
                                .header("X-IWM-Quota-Limit", Long.toString(props.getLimit()))
                                .header("X-IWM-Quota-Remaining", "0")
                                .header("X-IWM-Quota-Window-Seconds", Long.toString(windowSeconds))
                                .body(ApiResponse.failure("Tenant request quota exceeded for service " + serviceCode + ".",
                                        "TENANT_REQUEST_QUOTA_EXCEEDED"));
                    }
                    return ResponseEntity.ok()
                            .header("X-IWM-Quota-Limit", Long.toString(props.getLimit()))
                            .header("X-IWM-Quota-Remaining", Long.toString(remaining))
                            .header("X-IWM-Quota-Window-Seconds", Long.toString(windowSeconds))
                            .body(ApiResponse.success(body, "Authorized."));
                });
    }

    private String normalize(String serviceCode) {
        if (serviceCode == null || serviceCode.isBlank()) {
            return "";
        }
        return serviceCode.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
