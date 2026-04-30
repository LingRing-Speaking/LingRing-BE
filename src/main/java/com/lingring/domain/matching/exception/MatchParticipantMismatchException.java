package com.lingring.domain.matching.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;
import java.util.UUID;

public class MatchParticipantMismatchException extends DomainException {

    public MatchParticipantMismatchException(final UUID roomId, final Long userId) {
        super(
                ErrorCode.MATCH_PARTICIPANT_MISMATCH,
                "userId %d는 roomId %s 매칭의 참여자가 아닙니다.".formatted(userId, roomId)
        );
    }
}
