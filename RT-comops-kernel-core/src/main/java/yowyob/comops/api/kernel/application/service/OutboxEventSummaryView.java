package yowyob.comops.api.kernel.application.service;

public record OutboxEventSummaryView(
        String tenantId,
        long pendingCount,
        long publishedCount,
        long deadLetterCount) {
}
