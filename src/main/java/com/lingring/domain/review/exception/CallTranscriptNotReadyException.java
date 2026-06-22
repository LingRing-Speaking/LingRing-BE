package com.lingring.domain.review.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class CallTranscriptNotReadyException extends DomainException {

    public CallTranscriptNotReadyException(final Long callId) {
        super(
                ErrorCode.CALL_TRANSCRIPT_NOT_READY,
                "callId %d 통화의 transcript STT가 아직 완료되지 않았습니다.".formatted(callId)
        );
    }
}
