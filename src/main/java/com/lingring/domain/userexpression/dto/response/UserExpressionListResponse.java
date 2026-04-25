package com.lingring.domain.savedexpression.dto.response;

import com.lingring.domain.savedexpression.domain.SavedExpression;
import java.util.List;
import org.springframework.data.domain.Slice;

public record SavedExpressionListResponse(
        List<SavedExpressionResponse> items,
        boolean hasNext
) {

    public static SavedExpressionListResponse from(final Slice<SavedExpression> slice) {
        final List<SavedExpressionResponse> items = slice.getContent().stream()
                .map(SavedExpressionResponse::from)
                .toList();
        return new SavedExpressionListResponse(items, slice.hasNext());
    }
}
