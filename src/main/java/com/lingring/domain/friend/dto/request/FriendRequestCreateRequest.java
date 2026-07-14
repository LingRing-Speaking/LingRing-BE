package com.lingring.domain.friend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FriendRequestCreateRequest(
        @NotNull(message = "targetUserId가 비어있습니다.")
        @Positive(message = "targetUserId는 양수여야 합니다.")
        Long targetUserId
) {
}
