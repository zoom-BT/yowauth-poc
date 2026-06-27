package yowyob.comops.api.kernel.config;

import yowyob.comops.api.kernel.application.service.ClientApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BootstrapClientApplicationInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapClientApplicationInitializer.class);

    private final SecurityRuntimeProperties securityRuntimeProperties;
    private final ClientApplicationService clientApplicationService;

    public BootstrapClientApplicationInitializer(
            SecurityRuntimeProperties securityRuntimeProperties,
            ClientApplicationService clientApplicationService) {
        this.securityRuntimeProperties = securityRuntimeProperties;
        this.clientApplicationService = clientApplicationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        SecurityRuntimeProperties.BootstrapClientProperties bootstrap =
                securityRuntimeProperties.getClientApplications().getBootstrap();
        if (bootstrap == null || !bootstrap.isEnabled()) {
            return;
        }
        clientApplicationService.ensureBootstrapClient(bootstrap)
                .doOnNext(clientApplication -> LOGGER.info("Bootstrap client application ready: {}", clientApplication.clientId()))
                .doOnError(exception -> LOGGER.error("Failed to prepare bootstrap client application.", exception))
                .subscribe();
    }
}
