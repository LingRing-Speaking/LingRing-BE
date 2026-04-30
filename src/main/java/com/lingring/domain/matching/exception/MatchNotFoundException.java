package com.lingring.domain.matching.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.util.UUID;

public class MatchNotFoundException extends NotFoundException {

    public MatchNotFoundException(final UUID roomId) {
        super(ErrorCode.MATCH_NOT_FOUND, "roomId가 %s인 매칭을 찾을 수 없습니다.".formatted(roomId));
    }
}
