package com.lingring.global.auth.apple;

import java.util.ArrayList;
import java.util.List;

public class FakeAppleAuthClient implements AppleAuthClient {

    private final List<String> exchangedCodes = new ArrayList<>();
    private final List<String> revokedTokens = new ArrayList<>();
    private String nextRefreshToken = "fake-apple-refresh-token";
    private boolean throwOnExchange = false;
    private boolean throwOnRevoke = false;

    @Override
    public String exchangeAuthorizationCode(final String authorizationCode) {
        if (throwOnExchange) {
            throw new IllegalStateException("fake: exchange failed");
        }
        exchangedCodes.add(authorizationCode);
        return nextRefreshToken;
    }

    @Override
    public void revoke(final String refreshToken) {
        if (throwOnRevoke) {
            throw new IllegalStateException("fake: revoke failed");
        }
        revokedTokens.add(refreshToken);
    }

    public void setNextRefreshToken(final String value) {
        this.nextRefreshToken = value;
    }

    public void failNextExchange() {
        this.throwOnExchange = true;
    }

    public void failNextRevoke() {
        this.throwOnRevoke = true;
    }

    public List<String> exchangedCodes() {
        return List.copyOf(exchangedCodes);
    }

    public List<String> revokedTokens() {
        return List.copyOf(revokedTokens);
    }

    public void reset() {
        exchangedCodes.clear();
        revokedTokens.clear();
        nextRefreshToken = "fake-apple-refresh-token";
        throwOnExchange = false;
        throwOnRevoke = false;
    }
}
