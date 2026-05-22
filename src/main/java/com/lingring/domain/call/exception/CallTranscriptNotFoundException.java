package com.lingring.domain.call.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;

public class CallTranscriptNotFoundException extends NotFoundException {

    public CallTranscriptNotFoundException(final Long callId) {
        super(
                ErrorCode.CALL_TRANSCRIPT_NOT_FOUND,
                "callId가 %d인 transcript를 찾을 수 없습니다.".formatted(callId)
        );
    }
}
