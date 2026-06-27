package yowyob.comops.api.kernel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.outbox")
public class OutboxRuntimeGuardProperties {

    private boolean allowManualRelayOnly;

    public boolean isAllowManualRelayOnly() {
        return allowManualRelayOnly;
    }

    public void setAllowManualRelayOnly(boolean allowManualRelayOnly) {
        this.allowManualRelayOnly = allowManualRelayOnly;
    }
}
