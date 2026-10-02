package com.lingring.domain.auth.dto.response;

import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.dto.response.UserSummaryResponse;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        UserSummaryResponse user
) {

    public static AuthTokenResponse of(
            final String accessToken,
            final String refreshToken,
            final User user
    ) {
        return new AuthTokenResponse(accessToken, refreshToken, UserSummaryResponse.from(user));
    }
}
