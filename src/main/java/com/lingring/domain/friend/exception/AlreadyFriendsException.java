package com.lingring.domain.friend.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class AlreadyFriendsException extends DomainException {

    public AlreadyFriendsException(final Long userId, final Long otherUserId) {
        super(
                ErrorCode.ALREADY_FRIENDS,
                "userId가 %d와 %d인 사용자는 이미 친구입니다.".formatted(userId, otherUserId)
        );
    }
}
