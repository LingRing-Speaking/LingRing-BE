package com.lingring.domain.user.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class NicknameConflictException extends DomainException {

    public NicknameConflictException(final String nickname) {
        super(
                ErrorCode.NICKNAME_CONFLICT,
                "닉네임 '%s'은(는) 이미 사용 중입니다.".formatted(nickname)
        );
    }
}
