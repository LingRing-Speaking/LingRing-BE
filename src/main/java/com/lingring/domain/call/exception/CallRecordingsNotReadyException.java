package com.lingring.domain.call.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;

public class CallRecordingsNotReadyException extends BadRequestException {

    public CallRecordingsNotReadyException(final Long callId) {
        super(
                ErrorCode.CALL_RECORDINGS_NOT_READY,
                "callId %d 통화의 두 화자 녹음이 모두 업로드되어야 분석을 시작할 수 있습니다.".formatted(callId)
        );
    }
}
