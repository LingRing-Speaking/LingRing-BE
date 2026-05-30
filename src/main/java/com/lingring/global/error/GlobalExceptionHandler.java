package com.lingring.global.error;

import com.lingring.global.common.response.ApiResponse;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.DomainException;
import com.lingring.global.error.exception.ForbiddenException;
import com.lingring.global.error.exception.IdpUnavailableException;
import com.lingring.global.error.exception.InternalServerException;
import com.lingring.global.error.exception.InvalidValueException;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.error.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ApiResponse<Void> handleBadRequestException(final BadRequestException e) {
        log.info(e.getMessage());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        log.info("Validation failed: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ApiResponse<Void> handleUnauthorizedException(final UnauthorizedException e) {
        log.warn(e.getMessage());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ApiResponse<Void> handleForbiddenException(final ForbiddenException e) {
        log.warn(e.getMessage());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(NotFoundException.class)
    public ApiResponse<Void> handleNotFoundException(final NotFoundException e) {
        log.info(e.getMessage());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(InvalidValueException.class)
    public ApiResponse<Void> handleIllegalArgumentException(final InvalidValueException e) {
        log.info(e.getMessage());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(DomainException.class)
    public ApiResponse<Void> handleDomainException(final DomainException e) {
        log.error(e.getMessage());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(IdpUnavailableException.class)
    public ApiResponse<Void> handleIdpUnavailableException(final IdpUnavailableException e) {
        log.error(e.getMessage(), e);
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(InternalServerException.class)
    public ApiResponse<Void> handleInternalServerException(final InternalServerException e) {
        log.error(e.getMessage(), e);
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(final Exception e) {
        final ErrorCode errorCode = ErrorCode.SERVER_ERROR;
        log.error(errorCode.getMessage(), e);
        return ApiResponse.error(errorCode);
    }
}
