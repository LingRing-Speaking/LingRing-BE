package com.lingring.domain.expression.dto.response;

import com.lingring.domain.expression.domain.RecommendedExpression;
import java.time.LocalDateTime;

public record RecommendedExpressionResponse(
        Long id,
        String expression,
        String meaning,
        LocalDateTime createdAt,
        /** 호출자가 찜(저장)했으면 해당 저장 표현 row id, 아니면 null. */
        Long bookmarkId
) {

    public static RecommendedExpressionResponse from(
            final RecommendedExpression recommendedExpression,
            final Long bookmarkId
    ) {
        return new RecommendedExpressionResponse(
                recommendedExpression.getId(),
                recommendedExpression.getExpression().getValue(),
                recommendedExpression.getMeaning().getValue(),
                recommendedExpression.getCreatedAt(),
                bookmarkId
        );
    }
}
