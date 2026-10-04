package com.lingring.domain.referral.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class ReferralPeriodExpiredException extends DomainException {

    public ReferralPeriodExpiredException(final Long userId) {
        super(
                ErrorCode.REFERRAL_PERIOD_EXPIRED,
                "ID가 %d인 사용자는 추천인 입력 기간(가입 후 7일)이 지났습니다.".formatted(userId)
        );
    }
}
