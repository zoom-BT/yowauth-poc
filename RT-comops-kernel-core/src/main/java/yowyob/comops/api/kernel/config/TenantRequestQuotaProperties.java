package yowyob.comops.api.kernel.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.quotas.tenant-requests")
public class TenantRequestQuotaProperties {

    private boolean enabled;
    private long limit = 600;
    private Duration window = Duration.ofMinutes(1);
    private boolean failOpen = true;
    private String keyPrefix = "iwm:quotas:tenant-requests";
    private String coreServiceCode = "CORE";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getCoreServiceCode() {
        return coreServiceCode;
    }

    public void setCoreServiceCode(String coreServiceCode) {
        this.coreServiceCode = coreServiceCode;
    }
}
