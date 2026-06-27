package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.common.domain.model.ApiResponse;
import yowyob.comops.api.kernel.domain.ClientApplicationNotFoundException;
import yowyob.comops.api.kernel.domain.DuplicateClientApplicationIdException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice(basePackageClasses = ClientApplicationController.class)
public class ClientApplicationExceptionHandler {

    @ExceptionHandler(DuplicateClientApplicationIdException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleDuplicate(DuplicateClientApplicationIdException exception) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), "CLIENT_APPLICATION_ALREADY_EXISTS")));
    }

    @ExceptionHandler(ClientApplicationNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleNotFound(ClientApplicationNotFoundException exception) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(exception.getMessage(), "CLIENT_APPLICATION_NOT_FOUND")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleValidation(IllegalArgumentException exception) {
        return Mono.just(ResponseEntity.badRequest()
                .body(ApiResponse.failure(exception.getMessage(), "INVALID_CLIENT_APPLICATION_REQUEST")));
    }
}
