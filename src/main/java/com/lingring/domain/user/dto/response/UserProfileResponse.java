package com.lingring.domain.user.dto.response;

import com.lingring.domain.friend.domain.FriendRelation;
import com.lingring.domain.user.dao.dto.UserProfileProjection;
import com.lingring.domain.user.domain.Level;
import java.math.BigDecimal;

public record UserProfileResponse(
        Long id,
        String nickname,
        String profileImage,
        Level level,
        BigDecimal mannerTemperature,
        FriendRelation relation
) {

    public static UserProfileResponse of(final UserProfileProjection projection, final FriendRelation relation) {
        return new UserProfileResponse(
                projection.getId(),
                projection.getNickname(),
                projection.getProfileImage(),
                Level.valueOf(projection.getLevel()),
                projection.getMannerTemperature(),
                relation
        );
    }
}
