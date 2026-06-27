package yowyob.comops.api.kernel.application.service;

public record OperationalRuntimeView(
        String persistenceMode,
        String outboxDeliveryType,
        boolean outboxRelayEnabled,
        String outboxConsumersMode,
        boolean redisPermissionCacheEnabled,
        boolean elasticsearchSearchEnabled,
        boolean bootstrapClientEnabled) {
}
