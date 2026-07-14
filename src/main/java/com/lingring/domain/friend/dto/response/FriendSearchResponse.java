package com.lingring.domain.friend.dto.response;

import com.lingring.domain.friend.domain.FriendRelation;
import com.lingring.domain.user.dao.dto.UserSearchProjection;

public record FriendSearchResponse(
        Long userId,
        String nickname,
        String profileImage,
        FriendRelation relation
) {

    public static FriendSearchResponse of(final UserSearchProjection projection, final FriendRelation relation) {
        return new FriendSearchResponse(
                projection.getId(),
                projection.getNickname(),
                projection.getProfileImage(),
                relation
        );
    }
}
