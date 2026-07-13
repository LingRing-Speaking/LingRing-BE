package com.lingring.domain.friend.dto.response;

import com.lingring.domain.friend.dao.dto.FriendItemProjection;
import com.lingring.domain.friend.domain.FriendRequestDirection;
import com.lingring.domain.friend.domain.FriendshipStatus;
import java.time.LocalDateTime;

public record FriendItemResponse(
        Long userId,
        String nickname,
        String profileImage,
        FriendshipStatus status,
        FriendRequestDirection direction,
        LocalDateTime requestedAt
) {

    public static FriendItemResponse from(final FriendItemProjection projection) {
        return new FriendItemResponse(
                projection.getUserId(),
                projection.getNickname(),
                projection.getProfileImage(),
                projection.getStatus(),
                FriendRequestDirection.valueOf(projection.getDirection()),
                projection.getRequestedAt()
        );
    }
}
