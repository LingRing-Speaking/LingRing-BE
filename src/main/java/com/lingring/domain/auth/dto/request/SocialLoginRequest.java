package com.lingring.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        String provider,
        @NotBlank(message = "idToken이 비어있습니다.") String idToken,
        String accessToken,
        String nickname,
        String authorizationCode
) {
}
