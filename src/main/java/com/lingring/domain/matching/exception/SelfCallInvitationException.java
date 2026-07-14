package com.lingring.domain.matching.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class SelfCallInvitationException extends DomainException {

    public SelfCallInvitationException(final Long userId) {
        super(
                ErrorCode.SELF_CALL_INVITATION_NOT_ALLOWED,
                "userId가 %d인 사용자가 자기 자신에게 통화 초대를 보내려 했습니다.".formatted(userId)
        );
    }
}
