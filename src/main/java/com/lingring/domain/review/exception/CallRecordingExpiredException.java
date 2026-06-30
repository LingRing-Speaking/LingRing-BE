package com.lingring.domain.review.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class CallRecordingExpiredException extends DomainException {

    public CallRecordingExpiredException(final Long callId) {
        super(
                ErrorCode.CALL_RECORDING_EXPIRED,
                "callId %d 통화는 녹음 보관 기간이 지나 분석할 수 없습니다.".formatted(callId)
        );
    }
}
