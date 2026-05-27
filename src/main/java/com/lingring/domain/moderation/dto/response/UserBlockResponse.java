package com.lingring.domain.moderation.dto.response;

import com.lingring.domain.moderation.domain.UserBlock;
import java.time.LocalDateTime;

public record UserBlockResponse(
        Long id,
        Long userId,
        Long blockedUserId,
        LocalDateTime createdAt
) {

    public static UserBlockResponse from(final UserBlock userBlock) {
        return new UserBlockResponse(
                userBlock.getId(),
                userBlock.getUserId(),
                userBlock.getBlockedUserId(),
                userBlock.getCreatedAt()
        );
    }
}
