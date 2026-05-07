package com.lingring.global.auth.apple;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.IdpUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class RestClientAppleAuthClient implements AppleAuthClient {

    private final RestClient appleRestClient;
    private final AppleClientSecretSigner clientSecretSigner;
    private final AppleAuthProperties properties;

    @Override
    public String exchangeAuthorizationCode(final String authorizationCode) {
        final AppleAuthForm form = newAuthenticatedForm()
                .withAuthorizationCode(authorizationCode);

        final AppleTokenResponse response = appleRestClient.post()
                .uri(properties.tokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form.asMultiValueMap())
                .retrieve()
                .body(AppleTokenResponse.class);

        if (response == null || response.refreshToken() == null || response.refreshToken().isBlank()) {
            throw new IdpUnavailableException(
                    ErrorCode.IDP_UNAVAILABLE,
                    "Apple /auth/token 응답에 refresh_token이 없습니다."
            );
        }
        return response.refreshToken();
    }

    @Override
    public void revoke(final String refreshToken) {
        final AppleAuthForm form = newAuthenticatedForm()
                .withRefreshToken(refreshToken);

        appleRestClient.post()
                .uri(properties.revokeEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form.asMultiValueMap())
                .retrieve()
                .toBodilessEntity();
    }

    private AppleAuthForm newAuthenticatedForm() {
        return new AppleAuthForm()
                .withCredentials(properties.clientId(), clientSecretSigner.sign());
    }
}
