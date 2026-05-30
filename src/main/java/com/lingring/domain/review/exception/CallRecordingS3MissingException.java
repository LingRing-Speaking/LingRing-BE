package com.lingring.domain.review.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class CallRecordingS3MissingException extends DomainException {

    public CallRecordingS3MissingException(final String recordingKey) {
        super(
                ErrorCode.CALL_RECORDING_S3_MISSING,
                "업로드된 녹음 파일을 찾을 수 없습니다: %s".formatted(recordingKey)
        );
    }
}
