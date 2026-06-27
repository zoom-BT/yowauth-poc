package yowyob.comops.api.kernel.application.port.out;

import java.util.UUID;

public record CanonicalBankAccountAllocation(
        UUID id,
        UUID organizationId,
        UUID ownerThirdPartyId,
        String bankName,
        String accountNumber,
        String iban,
        String currency,
        String status) {
}
