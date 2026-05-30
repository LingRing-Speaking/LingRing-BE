package com.lingring.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken이 비어있습니다.") String refreshToken
) {
}