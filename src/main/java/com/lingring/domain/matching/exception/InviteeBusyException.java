package com.lingring.domain.matching.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class InviteeBusyException extends DomainException {

    public InviteeBusyException(final Long inviteeId) {
        super(
                ErrorCode.CALL_INVITEE_BUSY,
                "userId가 %d인 수신자가 이미 다른 통화 초대를 받는 중입니다.".formatted(inviteeId)
        );
    }
}
