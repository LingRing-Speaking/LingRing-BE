package com.lingring.domain.auth.service;

public record TokenIssuance(
        Long userId,
        String accessToken,
        String refreshToken
) {
}