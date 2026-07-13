package com.lingring.domain.friend.dto.request;

import com.lingring.domain.friend.domain.FriendshipStatus;
import jakarta.validation.constraints.NotNull;

public record FriendshipUpdateRequest(
        @NotNull(message = "status가 비어있습니다.")
        FriendshipStatus status
) {
}
