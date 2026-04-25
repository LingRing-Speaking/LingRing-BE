package com.lingring.domain.recommendedexpression.dto.response;

import com.lingring.domain.recommendedexpression.domain.RecommendedExpression;
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
