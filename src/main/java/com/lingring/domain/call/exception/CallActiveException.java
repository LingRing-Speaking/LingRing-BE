package com.lingring.domain.call.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class CallActiveException extends DomainException {

    public CallActiveException(final Long callId) {
        super(
                ErrorCode.CALL_ACTIVE,
                "callId %d 통화가 아직 진행 중이라 녹음 업로드를 시작할 수 없습니다.".formatted(callId)
        );
    }
}
