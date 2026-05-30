package com.lingring.global.error.exception;

import com.lingring.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidValueException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String message;

    public InvalidValueException(final ErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }

}
