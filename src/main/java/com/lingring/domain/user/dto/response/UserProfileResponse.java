package com.lingring.domain.user.dto.response;

import com.lingring.domain.user.dao.dto.UserProfileProjection;
import com.lingring.domain.user.domain.Level;
import java.math.BigDecimal;

public record UserProfileResponse(
        Long id,
        String nickname,
        String profileImage,
        Level level,
        BigDecimal mannerTemperature
) {

    public static UserProfileResponse from(final UserProfileProjection projection) {
        return new UserProfileResponse(
                projection.getId(),
                projection.getNickname(),
                projection.getProfileImage(),
                Level.valueOf(projection.getLevel()),
                projection.getMannerTemperature()
        );
    }
}