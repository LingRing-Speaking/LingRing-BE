package com.lingring.domain.savedexpression.dto.request;

public record SavedExpressionCreateRequest(
        String expression,
        String meaning
) {
}
