package yowyob.comops.api.auth.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import yowyob.comops.api.auth.config.AuthEmailProperties;

@Component
public class AuthEmailDeliveryService {

    private final JavaMailSender mailSender;
    private final AuthEmailProperties properties;

    public AuthEmailDeliveryService(org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSender,
            AuthEmailProperties properties) {
        this.mailSender = mailSender.getIfAvailable();
        this.properties = properties;
    }

    public DeliveryResult deliverPasswordReset(String recipientEmail, String token, long expiresInSeconds) {
        String link = buildLink(properties.getResetPasswordPath(), token);
        String subject = prefix("Password reset");
        String body = """
                A password reset has been requested for your account.

                Reset link:
                %s

                Reset token:
                %s

                This token expires in %d seconds.
                """.formatted(link, token, expiresInSeconds);
        return deliver(recipientEmail, subject, body, token, expiresInSeconds);
    }

    public DeliveryResult deliverEmailVerification(String recipientEmail, String token, long expiresInSeconds) {
        String link = buildLink(properties.getVerifyEmailPath(), token);
        String subject = prefix("Verify your email");
        String body = """
                Confirm your email address for your IWM account.

                Verification link:
                %s

                Verification token:
                %s

                This token expires in %d seconds.
                """.formatted(link, token, expiresInSeconds);
        return deliver(recipientEmail, subject, body, token, expiresInSeconds);
    }

    public DeliveryResult deliverLoginMfaCode(String recipientEmail, String code, long expiresInSeconds) {
        String subject = prefix("Your sign-in code");
        String body = """
                Use this verification code to finish signing in:

                %s

                This code expires in %d seconds. If you did not try to sign in, ignore this email.
                """.formatted(code, expiresInSeconds);
        return deliver(recipientEmail, subject, body, code, expiresInSeconds);
    }

    private DeliveryResult deliver(String recipientEmail, String subject, String body, String token, long expiresInSeconds) {
        if (!properties.isEnabled() || mailSender == null || properties.getFrom() == null || properties.getFrom().isBlank()) {
            return DeliveryResult.preview(token, expiresInSeconds);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        return DeliveryResult.smtp(expiresInSeconds);
    }

    private String buildLink(String path, String token) {
        String base = properties.getPublicBaseUrl() == null ? "" : properties.getPublicBaseUrl().replaceAll("/+$", "");
        String normalizedPath = path == null || path.isBlank() ? "" : (path.startsWith("/") ? path : "/" + path);
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return base + normalizedPath + "?token=" + encodedToken;
    }

    private String prefix(String subject) {
        String prefix = properties.getSubjectPrefix();
        return (prefix == null || prefix.isBlank()) ? subject : prefix.trim() + " " + subject;
    }

    public record DeliveryResult(
            String deliveryMode,
            String challengeTokenPreview,
            long expiresInSeconds) {

        public static DeliveryResult preview(String token, long expiresInSeconds) {
            return new DeliveryResult("PREVIEW_ONLY", token, expiresInSeconds);
        }

        public static DeliveryResult smtp(long expiresInSeconds) {
            return new DeliveryResult("SMTP", null, expiresInSeconds);
        }
    }
}
