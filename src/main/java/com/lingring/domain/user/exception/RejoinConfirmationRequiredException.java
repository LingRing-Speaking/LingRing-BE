package com.lingring.domain.user.exception;

import com.lingring.domain.user.domain.Provider;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class RejoinConfirmationRequiredException extends DomainException {

    public RejoinConfirmationRequiredException(final Provider provider) {
        super(
                ErrorCode.REJOIN_CONFIRMATION_REQUIRED,
                "%s 계정의 탈퇴 이력이 있어 재가입 확인이 필요합니다.".formatted(provider)
        );
    }
}
