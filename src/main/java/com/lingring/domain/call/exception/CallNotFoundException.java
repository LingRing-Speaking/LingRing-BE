package com.lingring.domain.call.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.util.UUID;

public class CallNotFoundException extends NotFoundException {

    public CallNotFoundException(final UUID roomId) {
        super(ErrorCode.CALL_NOT_FOUND,
                "roomId가 %s인 통화를 찾을 수 없습니다.".formatted(roomId));
    }

    public CallNotFoundException(final Long callId) {
        super(ErrorCode.CALL_NOT_FOUND,
                "callId가 %d인 통화를 찾을 수 없습니다.".formatted(callId));
    }
}
