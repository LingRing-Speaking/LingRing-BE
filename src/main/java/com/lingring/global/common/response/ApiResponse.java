package com.lingring.global.common.response;

import com.lingring.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
        T data,
        int status,
        String message
) {

    public static <T> ApiResponse<T> success(final HttpStatus httpStatus, final T data) {
        return new ApiResponse<>(data, httpStatus.value(), httpStatus.name());
    }

    public static ApiResponse<Void> success(final HttpStatus httpStatus) {
        return success(httpStatus, null);
    }

    public static ApiResponse<Void> error(final ErrorCode errorCode) {
        return new ApiResponse<>(null, errorCode.getHttpStatus().value(), errorCode.getMessage());
    }
}
