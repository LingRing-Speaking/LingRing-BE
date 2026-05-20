package com.lingring.domain.call.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class CallRecordingKeyForbiddenException extends DomainException {

    public CallRecordingKeyForbiddenException(final String recordingKey) {
        super(
                ErrorCode.CALL_RECORDING_KEY_FORBIDDEN,
                "본인의 녹음 키만 사용할 수 있습니다: %s".formatted(recordingKey)
        );
    }
}
