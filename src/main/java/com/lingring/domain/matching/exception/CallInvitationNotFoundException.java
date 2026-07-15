package com.lingring.domain.matching.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class CallInvitationNotFoundException extends DomainException {

    public CallInvitationNotFoundException(final Long inviteeId) {
        super(
                ErrorCode.CALL_INVITATION_NOT_FOUND,
                "userId가 %d인 수신자에게 수락하거나 거절할 통화 초대가 없습니다.".formatted(inviteeId)
        );
    }
}
