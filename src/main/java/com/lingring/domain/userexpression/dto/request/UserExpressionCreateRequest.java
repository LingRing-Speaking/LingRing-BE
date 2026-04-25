package com.lingring.domain.userexpression.dto.request;

public record UserExpressionCreateRequest(
        String expression,
        String meaning
) {
}
