package yowyob.comops.api.kernel.application.port.in;

import yowyob.comops.api.kernel.domain.model.TenantContext;
import reactor.core.publisher.Mono;

public interface CurrentTenantUseCase {

    Mono<TenantContext> currentTenant();
}
