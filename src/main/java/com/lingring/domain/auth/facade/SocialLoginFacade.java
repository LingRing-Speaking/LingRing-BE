package com.lingring.domain.auth.facade;

import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.service.AppleAuthService;
import com.lingring.domain.auth.service.AuthService;
import com.lingring.domain.auth.service.VerifiedIdToken;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialLoginFacade {

    private final AuthService authService;
    private final UserService userService;
    private final AppleAuthService appleAuthService;

    public AuthTokenResponse socialLogin(final SocialLoginRequest request) {
        final VerifiedIdToken verified = authService.verifyIdToken(
                request.provider(), request.idToken()
        );
        final User user = userService.findByProvider(verified.provider(), verified.providerUserId())
                .orElseGet(() -> userService.register(
                        verified.provider(), verified.providerUserId(), request.nickname()
                ));
        captureAppleCredentialIfPossible(verified.provider(), request.authorizationCode(), user.getId());
        return authService.issueTokensFor(user);
    }

    private void captureAppleCredentialIfPossible(
            final Provider provider,
            final String authorizationCode,
            final Long userId
    ) {
        if (provider != Provider.APPLE || authorizationCode == null || authorizationCode.isBlank()) {
            return;
        }
        appleAuthService.exchangeRefreshToken(authorizationCode)
                .ifPresent(refreshToken -> userService.updateAppleCredential(userId, refreshToken));
    }
}
