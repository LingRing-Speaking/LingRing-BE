package com.lingring.domain.auth.service;

import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.dto.response.MeResponse;
import com.lingring.domain.auth.dto.response.TokenPairResponse;
import com.lingring.domain.auth.exception.NicknameConflictException;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.auth.jwt.IdTokenVerifier;
import com.lingring.global.auth.jwt.IdTokenVerifiers;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final IdTokenVerifiers idTokenVerifiers;
    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;

    public AuthTokenResponse socialLogin(final SocialLoginRequest request) {
        final Provider provider = Provider.from(request.provider());
        final IdTokenVerifier verifier = idTokenVerifiers.resolve(provider);
        final String providerUserId = verifier.verify(request.idToken());
        final User user = userRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> registerNewUser(provider, providerUserId, request.nickname()));

        final TokenIssuance issued = tokenIssuer.issueFor(user.getId());
        return AuthTokenResponse.of(issued.accessToken(), issued.refreshToken(), user);
    }

    public TokenPairResponse refresh(final String refreshToken) {
        final TokenIssuance rotated = tokenIssuer.rotate(refreshToken);
        if (!userRepository.existsById(rotated.userId())) {
            tokenIssuer.invalidate(rotated.userId());
            throw new UnauthorizedException(
                    ErrorCode.INVALID_TOKEN,
                    "토큰 소유자를 찾을 수 없습니다. 다시 로그인하세요."
            );
        }
        return new TokenPairResponse(rotated.accessToken(), rotated.refreshToken());
    }

    public void logout(final Long userId) {
        tokenIssuer.invalidate(userId);
    }

    @Transactional(readOnly = true)
    public MeResponse me(final Long userId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.INVALID_TOKEN,
                        "토큰 소유자를 찾을 수 없습니다. 다시 로그인하세요."
                ));
        return MeResponse.from(user);
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
}