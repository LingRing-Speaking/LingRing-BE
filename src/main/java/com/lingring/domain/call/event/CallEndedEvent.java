package com.lingring.domain.call.event;

import java.time.LocalDateTime;

public record CallEndedEvent(
        Long userAId,
        Long userBId,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
