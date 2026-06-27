package yowyob.comops.api.roles.adapter.in.web;

import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.roles.domain.DuplicateRoleCodeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RoleController.class)
public class RoleExceptionHandler {

    @ExceptionHandler(DuplicateRoleCodeException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateCode(DuplicateRoleCodeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), "ROLE_CODE_DUPLICATE"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(exception.getMessage(), "ROLE_INVALID_REQUEST"));
    }
}
