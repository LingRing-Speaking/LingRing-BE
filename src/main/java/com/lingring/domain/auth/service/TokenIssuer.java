package com.lingring.domain.auth.service;

import com.lingring.domain.auth.dao.RefreshTokenRepository;
import com.lingring.global.auth.jwt.JwtProvider;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenIssuance issueFor(final Long userId) {
        final String access = jwtProvider.issueAccessToken(userId);
        final String refresh = jwtProvider.issueRefreshToken(userId);
        refreshTokenRepository.save(userId, refresh);
        return new TokenIssuance(userId, access, refresh);
    }

    public TokenIssuance rotate(final String refreshToken) {
        final Long userId = jwtProvider.parseRefreshToken(refreshToken);
        if (!refreshTokenRepository.exists(userId)) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_TOKEN,
                    "저장된 refresh token이 없습니다. 다시 로그인하세요."
            );
        }
        if (!refreshTokenRepository.matches(userId, refreshToken)) {
            refreshTokenRepository.deleteByUserId(userId);
            throw new UnauthorizedException(
                    ErrorCode.INVALID_TOKEN,
                    "이미 갱신된 refresh token입니다. 보안을 위해 모든 세션을 무효화했습니다."
            );
        }
        return issueFor(userId);
    }

    public void invalidate(final Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}