package com.lingring.domain.review.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class CallTooShortException extends DomainException {

    public CallTooShortException(final Long callId) {
        super(
                ErrorCode.CALL_TOO_SHORT,
                "callId %d 통화는 1분 미만이라 분석할 수 없습니다.".formatted(callId)
        );
    }
}
