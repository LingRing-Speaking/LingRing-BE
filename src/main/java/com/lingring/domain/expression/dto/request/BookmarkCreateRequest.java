package com.lingring.domain.expression.dto.request;

import com.lingring.domain.expression.domain.BookmarkSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BookmarkCreateRequest(
        @NotNull(message = "source가 비어있습니다.")
        BookmarkSource source,
        Long analysisId,
        @PositiveOrZero(message = "mistakeId는 0 이상이어야 합니다.")
        Integer mistakeId,
        Long recommendedExpressionId,
        Long icebreakerId
) {
}