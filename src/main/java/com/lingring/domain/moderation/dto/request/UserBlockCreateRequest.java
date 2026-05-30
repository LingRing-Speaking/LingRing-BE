package com.lingring.domain.moderation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserBlockCreateRequest(
        @NotNull(message = "blockedUserId가 비어있습니다.")
        @Positive(message = "blockedUserId는 양수여야 합니다.")
        Long blockedUserId
) {
}
