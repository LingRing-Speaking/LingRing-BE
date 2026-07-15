package com.lingring.domain.friend.exception;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.DomainException;

public class FriendshipAccessDeniedException extends DomainException {

    public FriendshipAccessDeniedException(final Long actorId) {
        super(
                ErrorCode.FRIENDSHIP_ACCESS_DENIED,
                "userId가 %d인 사용자가 권한이 없는 친구 관계를 변경하려 했습니다.".formatted(actorId)
        );
    }
}
