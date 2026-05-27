package com.lingring.domain.expression.dto.response;

import com.lingring.domain.expression.domain.RecommendedExpression;
import java.time.LocalDateTime;

public record RecommendedExpressionResponse(
        Long id,
        String expression,
        String meaning,
        LocalDateTime createdAt
) {

    public static RecommendedExpressionResponse from(
            final RecommendedExpression recommendedExpression
    ) {
        return new RecommendedExpressionResponse(
                recommendedExpression.getId(),
                recommendedExpression.getExpression().getValue(),
                recommendedExpression.getMeaning().getValue(),
                recommendedExpression.getCreatedAt()
        );
    }
}
