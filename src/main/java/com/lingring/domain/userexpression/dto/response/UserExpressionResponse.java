package com.lingring.domain.savedexpression.dto.response;

import com.lingring.domain.savedexpression.domain.SavedExpression;
import java.time.LocalDateTime;

public record SavedExpressionResponse(
        Long id,
        Long userId,
        String expression,
        String meaning,
        LocalDateTime createdAt
) {

    public static SavedExpressionResponse from(final SavedExpression savedExpression) {
        return new SavedExpressionResponse(
                savedExpression.getId(),
                savedExpression.getUserId(),
                savedExpression.getExpression().getValue(),
                savedExpression.getMeaning().getValue(),
                savedExpression.getCreatedAt()
        );
    }
}
