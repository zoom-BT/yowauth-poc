package yowyob.comops.api.kernel.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.security")
public class SecurityRuntimeProperties {

    private ClientApplicationsProperties clientApplications = new ClientApplicationsProperties();
    private JwtProperties jwt = new JwtProperties();

    public ClientApplicationsProperties getClientApplications() {
        return clientApplications;
    }

    public void setClientApplications(ClientApplicationsProperties clientApplications) {
        this.clientApplications = clientApplications;
    }

    public JwtProperties getJwt() {
        return jwt;
    }

    public void setJwt(JwtProperties jwt) {
        this.jwt = jwt;
    }

    public boolean isJwtConfigured() {
        return (jwt.privateKeyPath != null && !jwt.privateKeyPath.isBlank())
                || jwt.autoGenerateKeyPair;
    }

    public static class ClientApplicationsProperties {

        private BootstrapClientProperties bootstrap = new BootstrapClientProperties();

        public BootstrapClientProperties getBootstrap() {
            return bootstrap;
        }

        public void setBootstrap(BootstrapClientProperties bootstrap) {
            this.bootstrap = bootstrap;
        }
    }

    public static class BootstrapClientProperties {

        private boolean enabled;
        private String clientId;
        private String name = "Bootstrap Client";
        private String description = "Bootstrap server-to-server client application.";
        private String secret;
        private java.util.List<String> allowedServices = java.util.List.of();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public java.util.List<String> getAllowedServices() {
            return allowedServices;
        }

        public void setAllowedServices(java.util.List<String> allowedServices) {
            this.allowedServices = allowedServices;
        }
    }

    public static class JwtProperties {

        private String privateKeyPath;
        private String keyId = "iwm-key-1";
        private String issuer = "iwm-backend";
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private boolean autoGenerateKeyPair;

        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public boolean isAutoGenerateKeyPair() {
            return autoGenerateKeyPair;
        }

        public void setAutoGenerateKeyPair(boolean autoGenerateKeyPair) {
            this.autoGenerateKeyPair = autoGenerateKeyPair;
        }
    }
}
