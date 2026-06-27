package yowyob.comops.api.kernel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.security.jwt.deny-list")
public class JwtDenyListProperties {

    private boolean enabled = true;
    private String redisSetKey = "iwm:auth:jwt-denylist";
    private String redisChannel = "iwm.auth.jwt.revoked";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getRedisSetKey() { return redisSetKey; }
    public void setRedisSetKey(String v) { this.redisSetKey = v; }

    public String getRedisChannel() { return redisChannel; }
    public void setRedisChannel(String v) { this.redisChannel = v; }
}
