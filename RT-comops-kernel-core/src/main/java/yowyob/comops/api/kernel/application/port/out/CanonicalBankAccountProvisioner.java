package yowyob.comops.api.kernel.application.port.out;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface CanonicalBankAccountProvisioner {

    Mono<CanonicalBankAccountAllocation> ensureBankAccount(
            UUID tenantId,
            UUID organizationId,
            UUID ownerThirdPartyId,
            String purpose,
            String currency);
}
