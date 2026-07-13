package com.lingring.domain.friend.dto.response;

import com.lingring.domain.friend.domain.Friendship;
import com.lingring.domain.friend.domain.FriendshipStatus;

public record FriendshipResponse(
        Long userId,
        FriendshipStatus status
) {

    public static FriendshipResponse of(final Long userId, final Friendship friendship) {
        return new FriendshipResponse(userId, friendship.getStatus());
    }
}
