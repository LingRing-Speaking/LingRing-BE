package com.lingring.domain.callanalysis.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;

public class CallAnalysisNotFoundException extends NotFoundException {

    public CallAnalysisNotFoundException(final Long callId, final Long userId) {
        super(
                ErrorCode.CALL_ANALYSIS_NOT_FOUND,
                "callId=%d, userId=%d 인 통화 분석을 찾을 수 없습니다.".formatted(callId, userId)
        );
    }
}
