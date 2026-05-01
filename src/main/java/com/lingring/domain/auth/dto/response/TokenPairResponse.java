package com.lingring.domain.auth.dto.response;

public record TokenPairResponse(
        String accessToken,
        String refreshToken
) {
}