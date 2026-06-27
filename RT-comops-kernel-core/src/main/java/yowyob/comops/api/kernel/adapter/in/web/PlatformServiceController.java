package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.common.domain.model.PlatformServiceCode;
import yowyob.comops.api.kernel.application.service.ServiceCatalog;
import yowyob.comops.api.kernel.domain.model.ExternalServiceDefinition;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Catalogue des services de la plateforme : services NATIFS (enum) + services EXTERNES enregistrés.
 * Permet d'ajouter des services externes (ex. un nouveau backend) SANS toucher au code, pour qu'ils
 * puissent figurer dans les allowedServices d'une ClientApplication et être autorisés via /me/authorize.
 */
@RestController
@RequestMapping("/api/platform-services")
public class PlatformServiceController {

    private final ServiceCatalog serviceCatalog;

    public PlatformServiceController(ServiceCatalog serviceCatalog) {
        this.serviceCatalog = serviceCatalog;
    }

    public record CatalogServiceView(String code, String displayName, String description,
            boolean nativeService, boolean active) {
    }

    public record RegisterExternalServiceRequest(String code, String displayName, String description) {
    }

    /** Catalogue complet (natifs + externes), lisible par toute ClientApplication authentifiée. */
    @GetMapping
    @PreAuthorize("@businessAccessPolicy.isAuthenticatedClientApplication(authentication)")
    public Mono<ResponseEntity<ApiResponse<List<CatalogServiceView>>>> list() {
        List<CatalogServiceView> natives = new ArrayList<>(PlatformServiceCode.catalog().stream()
                .map(s -> new CatalogServiceView(s.code(), s.displayName(), s.description(), true, true))
                .toList());
        return serviceCatalog.registeredServices()
                .map(ext -> new CatalogServiceView(ext.code(), ext.displayName(), ext.description(), false, ext.active()))
                .collectList()
                .map(externals -> {
                    natives.addAll(externals);
                    return ResponseEntity.ok(ApiResponse.success(natives, "Platform services catalog."));
                });
    }

    /** Enregistre (ou met à jour) un service externe. Admin uniquement. */
    @PostMapping
    @PreAuthorize("@businessAccessPolicy.canManageClientApplications(authentication)")
    public Mono<ResponseEntity<ApiResponse<CatalogServiceView>>> register(
            @RequestBody Mono<RegisterExternalServiceRequest> requestMono) {
        return requestMono.flatMap(request -> serviceCatalog.register(
                        ExternalServiceDefinition.register(request.code(), request.displayName(), request.description())))
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                        new CatalogServiceView(saved.code(), saved.displayName(), saved.description(), false, saved.active()),
                        "External service registered.")));
    }

    /** Désactive un service externe (les ClientApplications ne pourront plus l'utiliser). Admin uniquement. */
    @DeleteMapping("/{code}")
    @PreAuthorize("@businessAccessPolicy.canManageClientApplications(authentication)")
    public Mono<ResponseEntity<ApiResponse<Void>>> deactivate(@PathVariable String code) {
        return serviceCatalog.deactivate(code)
                .thenReturn(ResponseEntity.ok(ApiResponse.success(null, "External service deactivated.")));
    }
}
