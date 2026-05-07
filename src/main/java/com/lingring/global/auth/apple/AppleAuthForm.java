package com.lingring.global.auth.apple;

import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class AppleAuthForm {

    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private static final String TOKEN_TYPE_HINT_REFRESH = "refresh_token";

    private final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

    public AppleAuthForm withCredentials(final String clientId, final String clientSecret) {
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        return this;
    }

    public AppleAuthForm withAuthorizationCode(final String authorizationCode) {
        form.add("code", authorizationCode);
        form.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
        return this;
    }

    public AppleAuthForm withRefreshToken(final String refreshToken) {
        form.add("token", refreshToken);
        form.add("token_type_hint", TOKEN_TYPE_HINT_REFRESH);
        return this;
    }

    public MultiValueMap<String, String> asMultiValueMap() {
        return CollectionUtils.unmodifiableMultiValueMap(form);
    }
}
