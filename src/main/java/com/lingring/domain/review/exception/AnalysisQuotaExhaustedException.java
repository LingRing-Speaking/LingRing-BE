package com.lingring.domain.review.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class AnalysisQuotaExhaustedException extends DomainException {

    public AnalysisQuotaExhaustedException(final Long userId) {
        super(
                ErrorCode.ANALYSIS_QUOTA_EXHAUSTED,
                "userId %d의 분석 티켓(무료+유료)을 모두 사용했습니다.".formatted(userId)
        );
    }
}
