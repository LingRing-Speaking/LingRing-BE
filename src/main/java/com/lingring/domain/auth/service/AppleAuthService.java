package com.lingring.domain.auth.service;

import com.lingring.global.auth.apple.AppleAuthClient;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleAuthService {

    private final AppleAuthClient appleAuthClient;

    public Optional<String> exchangeRefreshToken(final String authorizationCode) {
        try {
            return Optional.of(appleAuthClient.exchangeAuthorizationCode(authorizationCode));
        } catch (final Exception ex) {
            log.warn("Apple authorizationCode exchange 실패.", ex);
            return Optional.empty();
        }
    }

    public void revoke(final String refreshToken) {
        try {
            appleAuthClient.revoke(refreshToken);
        } catch (final Exception ex) {
            log.warn("Apple revoke 실패, 호출자에서 후속 처리 결정 필요.", ex);
        }
    }
}
