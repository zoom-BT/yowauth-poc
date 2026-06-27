package yowyob.comops.api.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.security.login-throttle")
public class LoginThrottleProperties {

    private boolean enabled = true;
    private int maxFailuresPerPrincipal = 5;
    private int maxFailuresPerIp = 20;
    private Duration principalWindow = Duration.ofMinutes(15);
    private Duration ipWindow = Duration.ofMinutes(15);
    private Duration principalLockout = Duration.ofMinutes(15);
    private Duration ipLockout = Duration.ofMinutes(30);
    private String keyPrefix = "iwm:auth:login-throttle";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMaxFailuresPerPrincipal() { return maxFailuresPerPrincipal; }
    public void setMaxFailuresPerPrincipal(int v) { this.maxFailuresPerPrincipal = v; }

    public int getMaxFailuresPerIp() { return maxFailuresPerIp; }
    public void setMaxFailuresPerIp(int v) { this.maxFailuresPerIp = v; }

    public Duration getPrincipalWindow() { return principalWindow; }
    public void setPrincipalWindow(Duration v) { this.principalWindow = v; }

    public Duration getIpWindow() { return ipWindow; }
    public void setIpWindow(Duration v) { this.ipWindow = v; }

    public Duration getPrincipalLockout() { return principalLockout; }
    public void setPrincipalLockout(Duration v) { this.principalLockout = v; }

    public Duration getIpLockout() { return ipLockout; }
    public void setIpLockout(Duration v) { this.ipLockout = v; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String v) { this.keyPrefix = v; }
}
