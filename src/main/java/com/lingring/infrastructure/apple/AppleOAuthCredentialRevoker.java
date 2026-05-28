package com.lingring.infrastructure.apple;

import com.lingring.domain.auth.service.AppleAuthService;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.port.OAuthCredentialRevoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleOAuthCredentialRevoker implements OAuthCredentialRevoker {

    private final AppleAuthService appleAuthService;

    @Override
    public void revoke(final Provider provider, final String refreshToken) {
        if (provider != Provider.APPLE) {
            return;
        }
        appleAuthService.revoke(refreshToken);
    }
}
