package yowyob.comops.api.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.auth.email")
public class AuthEmailProperties {

    private boolean enabled;
    private String from;
    private String subjectPrefix = "[IWM]";
    private String publicBaseUrl = "http://localhost:8080";
    private String resetPasswordPath = "/auth/reset-password";
    private String verifyEmailPath = "/auth/verify-email";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getResetPasswordPath() {
        return resetPasswordPath;
    }

    public void setResetPasswordPath(String resetPasswordPath) {
        this.resetPasswordPath = resetPasswordPath;
    }

    public String getVerifyEmailPath() {
        return verifyEmailPath;
    }

    public void setVerifyEmailPath(String verifyEmailPath) {
        this.verifyEmailPath = verifyEmailPath;
    }
}
