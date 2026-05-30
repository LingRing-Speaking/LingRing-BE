package com.lingring.domain.matching.domain;

import java.time.LocalDateTime;
import lombok.NonNull;

public record MatchingCandidate(
        @NonNull Long userId,
        @NonNull LocalDateTime enqueuedAt
) {
}
