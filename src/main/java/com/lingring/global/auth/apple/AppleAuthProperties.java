package com.lingring.global.auth.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.apple")
public record AppleAuthProperties(
        String teamId,
        String keyId,
        String clientId,
        String privateKey,
        String tokenEndpoint,
        String revokeEndpoint
) {

    private static final String DEFAULT_TOKEN_ENDPOINT = "https://appleid.apple.com/auth/token";
    private static final String DEFAULT_REVOKE_ENDPOINT = "https://appleid.apple.com/auth/revoke";

    public AppleAuthProperties {
        if (tokenEndpoint == null || tokenEndpoint.isBlank()) {
            tokenEndpoint = DEFAULT_TOKEN_ENDPOINT;
        }
        if (revokeEndpoint == null || revokeEndpoint.isBlank()) {
            revokeEndpoint = DEFAULT_REVOKE_ENDPOINT;
        }
    }
}
