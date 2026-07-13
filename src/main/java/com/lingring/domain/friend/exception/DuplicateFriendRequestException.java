package com.lingring.domain.friend.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class DuplicateFriendRequestException extends DomainException {

    public DuplicateFriendRequestException(final Long requesterId, final Long addresseeId) {
        super(
                ErrorCode.DUPLICATE_FRIEND_REQUEST,
                "userId가 %d인 사용자가 %d에게 보낸 친구 요청이 이미 존재합니다.".formatted(requesterId, addresseeId)
        );
    }
}
