package com.lingring.domain.user.event;

import com.lingring.domain.user.domain.WithdrawReason;

public record UserWithdrawnEvent(
        Long userId,
        WithdrawReason reason,
        String description
) {
}
