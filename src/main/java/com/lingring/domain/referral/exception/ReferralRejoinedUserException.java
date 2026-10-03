package com.lingring.domain.referral.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class ReferralRejoinedUserException extends DomainException {

    public ReferralRejoinedUserException(final Long userId) {
        super(
                ErrorCode.REFERRAL_NOT_ELIGIBLE_REJOINED,
                "ID가 %d인 사용자는 탈퇴 이력이 있는 재가입자라 추천인을 입력할 수 없습니다.".formatted(userId)
        );
    }
}
