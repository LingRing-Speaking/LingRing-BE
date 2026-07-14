package com.lingring.domain.matching.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class InviteeOfflineException extends DomainException {

    public InviteeOfflineException(final Long inviteeId) {
        super(
                ErrorCode.CALL_INVITEE_OFFLINE,
                "userId가 %d인 수신자가 오프라인이어서 통화 초대를 생성할 수 없습니다.".formatted(inviteeId)
        );
    }
}
