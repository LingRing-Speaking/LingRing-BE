package com.lingring.domain.matching.domain;

import java.util.UUID;
import lombok.NonNull;

public record MatchingResult(
        @NonNull Long partnerId,
        @NonNull UUID roomId
) {
}
