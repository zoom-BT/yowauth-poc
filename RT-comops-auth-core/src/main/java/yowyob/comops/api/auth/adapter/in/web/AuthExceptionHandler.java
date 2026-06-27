package yowyob.comops.api.auth.adapter.in.web;

import yowyob.comops.api.auth.application.service.LoginThrottleService;
import yowyob.comops.api.auth.application.service.RefreshTokenService;
import yowyob.comops.api.auth.domain.DuplicateEmailException;
import yowyob.comops.api.auth.domain.DuplicateUsernameException;
import yowyob.comops.api.auth.domain.EmailNotVerifiedException;
import yowyob.comops.api.auth.domain.InvalidLoginCredentialsException;
import yowyob.comops.api.common.domain.model.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AuthController.class, UserController.class, SessionTokensController.class})
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateUsername(DuplicateUsernameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), "USERNAME_DUPLICATE"));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), "EMAIL_DUPLICATE"));
    }

    @ExceptionHandler(InvalidLoginCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidLoginCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(exception.getMessage(), "AUTH_INVALID_CREDENTIALS"));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailNotVerified(EmailNotVerifiedException exception) {
        // 403 : identité valide mais email non vérifié -> aucune session tant que non confirmé.
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(exception.getMessage(), "EMAIL_NOT_VERIFIED"));
    }

    @ExceptionHandler(RefreshTokenService.InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshToken(
            RefreshTokenService.InvalidRefreshTokenException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(exception.getMessage(), "AUTH_INVALID_REFRESH_TOKEN"));
    }

    @ExceptionHandler(LoginThrottleService.LoginThrottledException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoginThrottled(LoginThrottleService.LoginThrottledException exception) {
        long seconds = Math.max(1, exception.getRetryAfter().toSeconds());
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(seconds));
        String code = "ip".equals(exception.getScope()) ? "AUTH_THROTTLED_IP" : "AUTH_THROTTLED_PRINCIPAL";
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(ApiResponse.failure("Too many failed login attempts. Try again later.", code));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(exception.getMessage(), "AUTH_INVALID_REQUEST"));
    }
}
