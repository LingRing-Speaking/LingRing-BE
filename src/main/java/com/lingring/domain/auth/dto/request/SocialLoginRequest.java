package com.lingring.domain.auth.dto.request;

public record SocialLoginRequest(
        String provider,
        String idToken,
        String accessToken,
        String nickname
) {
}
