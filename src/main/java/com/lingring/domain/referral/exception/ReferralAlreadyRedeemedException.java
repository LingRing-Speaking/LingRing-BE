package com.lingring.domain.referral.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class ReferralAlreadyRedeemedException extends DomainException {

    public ReferralAlreadyRedeemedException(final Long userId) {
        super(
                ErrorCode.REFERRAL_ALREADY_REDEEMED,
                "ID가 %d인 사용자는 이미 추천인을 입력했습니다.".formatted(userId)
        );
    }
}
