package com.lingring.domain.review.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.ForbiddenException;

public class CallAnalysisAccessForbiddenException extends ForbiddenException {

    public CallAnalysisAccessForbiddenException(final Long analysisId, final Long requesterId) {
        super(
                ErrorCode.CALL_ANALYSIS_ACCESS_FORBIDDEN,
                "analysisId=%d 에 대한 접근 권한이 없습니다. requesterId=%d".formatted(analysisId, requesterId)
        );
    }
}
