package com.lingring.domain.auth.service;

import com.lingring.domain.auth.dao.RefreshTokenRepository;
import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.exception.NicknameConflictException;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.auth.jwt.IdTokenVerifier;
import com.lingring.global.auth.jwt.JwtProvider;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.error.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final IdTokenVerifier idTokenVerifier;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthTokenResponse socialLogin(final SocialLoginRequest request) {
        validateRequest(request);
        final Provider provider = parseProvider(request.provider());
        if (provider != Provider.KAKAO) {
            throw new BadRequestException(
                    ErrorCode.NOT_SUPPORTED,
                    "지원하지 않는 provider입니다: %s".formatted(request.provider())
            );
        }

        final String providerUserId = idTokenVerifier.verify(request.idToken());

        final User user = userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> registerNewUser(provider, providerUserId, request.nickname()));

        return issueTokens(user);
    }

    public AuthTokenResponse refresh(final String refreshToken) {
        final Long userId = jwtProvider.parseRefreshToken(refreshToken);
        final String storedHash = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.INVALID_TOKEN,
                        "저장된 refresh token이 없습니다. 다시 로그인하세요."
                ));

        if (!hashOf(refreshToken).equals(storedHash)) {
            refreshTokenRepository.deleteByUserId(userId);
            throw new UnauthorizedException(
                    ErrorCode.INVALID_TOKEN,
                    "이미 갱신된 refresh token입니다. 보안을 위해 모든 세션을 무효화했습니다."
            );
        }

        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(userId)
                ));

        return issueTokens(user);
    }

    public void logout(final Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private void validateRequest(final SocialLoginRequest request) {
        if (request.idToken() == null || request.idToken().isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "idToken이 비어있습니다.");
        }
    }

    private User registerNewUser(
            final Provider provider,
            final String providerUserId,
            final String nickname
    ) {
        if (nickname == null || nickname.isBlank()) {
            throw new BadRequestException(
                    ErrorCode.NICKNAME_REQUIRED,
                    "신규 가입에는 nickname이 필요합니다."
            );
        }
        final Name name = new Name(nickname);
        if (userRepository.existsByName(name)) {
            throw new NicknameConflictException(nickname);
        }
        final User user = User.createFromOAuth(provider, providerUserId, name, null);
        return userRepository.save(user);
    }

    private AuthTokenResponse issueTokens(final User user) {
        final String access = jwtProvider.issueAccessToken(user.getId());
        final String refresh = jwtProvider.issueRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), hashOf(refresh));
        return AuthTokenResponse.of(access, refresh, user);
    }

    private static Provider parseProvider(final String provider) {
        if (provider == null || provider.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "provider가 비어있습니다.");
        }
        try {
            return Provider.valueOf(provider.toUpperCase());
        } catch (final IllegalArgumentException ex) {
            throw new BadRequestException(
                    ErrorCode.NOT_SUPPORTED,
                    "지원하지 않는 provider입니다: %s".formatted(provider)
            );
        }
    }

    private static String hashOf(final String token) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
