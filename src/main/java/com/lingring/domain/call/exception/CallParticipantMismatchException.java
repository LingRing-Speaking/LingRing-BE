package com.lingring.domain.call.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;
import java.util.UUID;

public class CallParticipantMismatchException extends DomainException {

    public CallParticipantMismatchException(final UUID roomId, final Long userId) {
        super(
                ErrorCode.CALL_PARTICIPANT_MISMATCH,
                "userId %d는 roomId %s 통화의 참여자가 아닙니다.".formatted(userId, roomId)
        );
    }

    public CallParticipantMismatchException(final Long callId, final Long userId) {
        super(
                ErrorCode.CALL_PARTICIPANT_MISMATCH,
                "userId %d는 callId %d 통화의 참여자가 아닙니다.".formatted(userId, callId)
        );
    }
}
