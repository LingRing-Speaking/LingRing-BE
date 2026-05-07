package com.lingring.global.error.exception;

import com.lingring.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class InternalServerException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String message;

    public InternalServerException(final ErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }

    public InternalServerException(final ErrorCode errorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.message = message;
    }
}
