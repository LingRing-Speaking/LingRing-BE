package com.lingring.domain.matching.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class CallInvitationAlreadySentException extends DomainException {

    public CallInvitationAlreadySentException(final Long inviterId) {
        super(
                ErrorCode.CALL_INVITATION_ALREADY_SENT,
                "userId가 %d인 발신자에게 이미 진행 중인 통화 초대가 있습니다.".formatted(inviterId)
        );
    }
}
