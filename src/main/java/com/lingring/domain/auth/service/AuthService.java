package com.lingring.domain.auth.service;

import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.dto.response.TokenPairResponse;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.global.auth.jwt.IdTokenVerifier;
import com.lingring.global.auth.jwt.IdTokenVerifiers;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final IdTokenVerifiers idTokenVerifiers;
    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;

    public VerifiedIdToken verifyIdToken(final String providerName, final String idToken) {
        final Provider provider = Provider.from(providerName);
        final IdTokenVerifier verifier = idTokenVerifiers.resolve(provider);
        final String providerUserId = verifier.verify(idToken);
        return new VerifiedIdToken(provider, providerUserId);
    }

    public AuthTokenResponse issueTokensFor(final User user) {
        final TokenIssuance issued = tokenIssuer.issueFor(user.getId());
        return AuthTokenResponse.of(issued.accessToken(), issued.refreshToken(), user);
    }

    @Transactional(readOnly = true)
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
}
