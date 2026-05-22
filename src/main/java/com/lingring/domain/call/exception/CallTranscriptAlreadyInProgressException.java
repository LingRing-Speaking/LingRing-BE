package com.lingring.domain.call.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class CallTranscriptAlreadyInProgressException extends DomainException {

    public CallTranscriptAlreadyInProgressException(final Long callId) {
        super(
                ErrorCode.CALL_TRANSCRIPT_ALREADY_IN_PROGRESS,
                "callId %d 통화의 분석이 이미 진행 중입니다.".formatted(callId)
        );
    }
}
