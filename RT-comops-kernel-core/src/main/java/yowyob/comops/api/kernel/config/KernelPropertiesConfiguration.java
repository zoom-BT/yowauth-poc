package yowyob.comops.api.kernel.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OutboxRelayProperties.class, KafkaOutboxDeliveryProperties.class,
        OutboxConsumersProperties.class, RedisPermissionCacheProperties.class, ElasticsearchSearchProperties.class,
        SecurityRuntimeProperties.class, OutboxRuntimeGuardProperties.class, KernelObservabilityProperties.class,
        ManagementSecurityProperties.class, OutboxReplayProperties.class, TenantRequestQuotaProperties.class,
        OrganizationServiceRequestQuotaProperties.class, JwtDenyListProperties.class,
        AuditIntegrityProperties.class})
public class KernelPropertiesConfiguration {
}
