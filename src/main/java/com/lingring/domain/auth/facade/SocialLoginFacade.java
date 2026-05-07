package com.lingring.domain.auth.facade;

import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.service.AuthService;
import com.lingring.domain.auth.service.VerifiedIdToken;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.service.UserService;
import com.lingring.global.auth.apple.AppleAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialLoginFacade {

    private final AuthService authService;
    private final UserService userService;
    private final AppleAuthClient appleAuthClient;

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
        try {
            final String refreshToken = appleAuthClient.exchangeAuthorizationCode(authorizationCode);
            userService.updateAppleCredential(userId, refreshToken);
        } catch (final Exception ex) {
            log.warn("Apple authorizationCode exchange 실패. userId={}", userId, ex);
        }
    }
}
