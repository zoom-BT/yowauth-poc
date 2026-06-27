package yowyob.comops.api.kernel.application.service;

import java.util.Map;

public record DomainProjectionSummaryView(
        String tenantId,
        long totalCount,
        Map<String, Long> perDomainCount) {
}
