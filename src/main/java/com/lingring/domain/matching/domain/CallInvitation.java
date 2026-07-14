package com.lingring.domain.matching.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.NonNull;

public record CallInvitation(
        @NonNull Long inviterId,
        @NonNull Long inviteeId,
        @NonNull UUID roomId,
        @NonNull LocalDateTime deadline
) {
}
