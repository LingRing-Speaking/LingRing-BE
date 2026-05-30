package com.lingring.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DemoLoginRequest(
        @NotBlank(message = "token이 비어있습니다.") String token
) {
}
