package com.lingring.domain.referral.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class ReferralSelfNotAllowedException extends DomainException {

    public ReferralSelfNotAllowedException(final Long userId) {
        super(
                ErrorCode.REFERRAL_SELF_NOT_ALLOWED,
                "ID가 %d인 사용자가 자기 자신을 추천인으로 입력했습니다.".formatted(userId)
        );
    }
}
