package com.lingring.domain.userexpression.dto.response;

import com.lingring.domain.userexpression.domain.UserExpression;
import java.time.LocalDateTime;

public record UserExpressionResponse(
        Long id,
        Long userId,
        String expression,
        String meaning,
        LocalDateTime createdAt
) {

    public static UserExpressionResponse from(final UserExpression userExpression) {
        return new UserExpressionResponse(
                userExpression.getId(),
                userExpression.getUserId(),
                userExpression.getExpression().getValue(),
                userExpression.getMeaning().getValue(),
                userExpression.getCreatedAt()
        );
    }
}
