package com.lingring.domain.friend.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class FriendshipNotFoundException extends DomainException {

    public FriendshipNotFoundException(final Long userId, final Long otherUserId) {
        super(
                ErrorCode.FRIENDSHIP_NOT_FOUND,
                "userId가 %d와 %d인 사용자 사이의 친구 관계를 찾을 수 없습니다.".formatted(userId, otherUserId)
        );
    }
}
