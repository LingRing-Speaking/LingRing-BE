package com.lingring.domain.expression.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserExpressionCreateRequest(
        @NotBlank(message = "expression이 비어있습니다.")
        @Size(max = 500, message = "expression은 500자 이하여야 합니다.")
        String expression,
        @NotBlank(message = "meaning이 비어있습니다.")
        @Size(max = 500, message = "meaning은 500자 이하여야 합니다.")
        String meaning
) {
}
