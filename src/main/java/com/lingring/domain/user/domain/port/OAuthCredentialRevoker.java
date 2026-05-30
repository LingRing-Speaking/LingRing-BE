package com.lingring.domain.user.domain.port;

import com.lingring.domain.user.domain.Provider;

public interface OAuthCredentialRevoker {

    void revoke(Provider provider, String refreshToken);
}
