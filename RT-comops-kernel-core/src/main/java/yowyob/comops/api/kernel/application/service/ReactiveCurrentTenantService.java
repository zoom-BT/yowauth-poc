package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.kernel.application.port.in.CurrentTenantUseCase;
import yowyob.comops.api.kernel.domain.model.TenantContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReactiveCurrentTenantService implements CurrentTenantUseCase {

    @Override
    public Mono<TenantContext> currentTenant() {
        return ReactiveRequestContextHolder.getRequiredContext();
    }
}
