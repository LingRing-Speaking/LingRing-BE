package com.lingring.domain.userblock.dto.response;

import com.lingring.domain.userblock.dao.dto.UserBlockItemProjection;
import java.time.LocalDateTime;

public record UserBlockItemResponse(
        Long id,
        Long blockedUserId,
        String nickname,
        String profileImage,
        LocalDateTime createdAt
) {

    public static UserBlockItemResponse from(final UserBlockItemProjection projection) {
        return new UserBlockItemResponse(
                projection.getId(),
                projection.getBlockedUserId(),
                projection.getNickname(),
                projection.getProfileImage(),
                projection.getCreatedAt()
        );
    }
}
