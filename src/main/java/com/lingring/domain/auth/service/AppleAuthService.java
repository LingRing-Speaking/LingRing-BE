package com.lingring.domain.auth.service;

import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.AppleOAuthCredential;
import com.lingring.domain.user.service.UserService;
import com.lingring.global.auth.apple.AppleAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleAuthService {

    private final AppleAuthClient appleAuthClient;
    private final UserService userService;

    public void captureRefreshToken(final Long userId, final String authorizationCode) {
        try {
            final String refreshToken = appleAuthClient.exchangeAuthorizationCode(authorizationCode);
            userService.updateAppleCredential(userId, refreshToken);
        } catch (final Exception ex) {
            log.warn("Apple authorizationCode exchange 실패. userId={}", userId, ex);
        }
    }

    public void revokeForUser(final User user) {
        final AppleOAuthCredential credential = user.getAppleCredential();
        if (credential == null) {
            log.info("Apple credential 없음, revoke 건너뜀. userId={}", user.getId());
            return;
        }
        try {
            appleAuthClient.revoke(credential.getRefreshToken());
        } catch (final Exception ex) {
            log.warn("Apple revoke 실패, 탈퇴 진행. userId={}", user.getId(), ex);
        }
    }
}
