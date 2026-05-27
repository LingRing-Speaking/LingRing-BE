package com.lingring.domain.expression.dto.response;

import com.lingring.domain.expression.domain.UserExpression;
import java.util.List;
import org.springframework.data.domain.Slice;

public record UserExpressionListResponse(
        List<UserExpressionResponse> items,
        boolean hasNext
) {

    public static UserExpressionListResponse from(final Slice<UserExpression> slice) {
        final List<UserExpressionResponse> items = slice.getContent().stream()
                .map(UserExpressionResponse::from)
                .toList();
        return new UserExpressionListResponse(items, slice.hasNext());
    }
}
