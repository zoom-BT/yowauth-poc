package yowyob.comops.api.actor.adapter.in.web;

import yowyob.comops.api.actor.domain.BusinessActorAlreadyExistsException;
import yowyob.comops.api.actor.domain.BusinessActorNotFoundException;
import yowyob.comops.api.actor.domain.BusinessActorSelfReactivationDisabledException;
import yowyob.comops.api.actor.domain.BusinessActorSelfReactivationNotAllowedException;
import yowyob.comops.api.actor.domain.DuplicateActorEmailException;
import yowyob.comops.api.actor.domain.UserAccountLinkNotFoundException;
import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.domain.MissingTenantException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ActorController.class)
public class ActorExceptionHandler {

    @ExceptionHandler(DuplicateActorEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(DuplicateActorEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), "ACTOR_EMAIL_DUPLICATE"));
    }

    @ExceptionHandler(BusinessActorAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessActorExists(BusinessActorAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), "BUSINESS_ACTOR_ALREADY_EXISTS"));
    }

    @ExceptionHandler({BusinessActorNotFoundException.class, UserAccountLinkNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleBusinessActorNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(exception.getMessage(), "BUSINESS_ACTOR_NOT_FOUND"));
    }

    @ExceptionHandler(BusinessActorSelfReactivationDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleSelfReactivationDisabled(
            BusinessActorSelfReactivationDisabledException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(exception.getMessage(), "BUSINESS_ACTOR_SELF_REACTIVATION_DISABLED"));
    }

    @ExceptionHandler(BusinessActorSelfReactivationNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSelfReactivationNotAllowed(
            BusinessActorSelfReactivationNotAllowedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), "BUSINESS_ACTOR_SELF_REACTIVATION_NOT_ALLOWED"));
    }

    @ExceptionHandler(MissingTenantException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingTenant(MissingTenantException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(exception.getMessage(), "TENANT_REQUIRED"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(exception.getMessage(), "ACTOR_INVALID_REQUEST"));
    }
}
