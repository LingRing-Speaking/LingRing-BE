package com.lingring.domain.call.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.util.UUID;

public class CallHistoryNotFoundException extends NotFoundException {

    public CallHistoryNotFoundException(final UUID roomId) {
        super(ErrorCode.CALL_HISTORY_NOT_FOUND,
                "roomId가 %s인 통화 기록을 찾을 수 없습니다.".formatted(roomId));
    }
}
