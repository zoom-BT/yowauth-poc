package yowyob.comops.api.common.domain.model;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, String message, String errorCode, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode, Instant.now());
    }
}
