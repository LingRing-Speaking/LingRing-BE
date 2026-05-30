package com.lingring.global.auth.apple;

public interface AppleAuthClient {

    String exchangeAuthorizationCode(String authorizationCode);

    void revoke(String refreshToken);
}
