package com.lingring.domain.matching.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class MatchConfirmationNotFoundException extends DomainException {

    public MatchConfirmationNotFoundException(final Long userId) {
        super(
                ErrorCode.MATCH_CONFIRMATION_NOT_FOUND,
                "userId=%d 에 대한 매칭 컨펌 레코드가 없습니다.".formatted(userId)
        );
    }
}
