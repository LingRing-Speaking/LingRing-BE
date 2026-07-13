package com.lingring.domain.friend.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class SelfFriendshipException extends DomainException {

    public SelfFriendshipException(final Long userId) {
        super(
                ErrorCode.SELF_FRIEND_REQUEST_NOT_ALLOWED,
                "userId가 %d인 사용자가 자기 자신에게 친구 요청을 보내려 했습니다.".formatted(userId)
        );
    }
}
