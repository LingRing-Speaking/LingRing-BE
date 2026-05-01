package com.lingring.domain.auth.dto.response;

import com.lingring.domain.user.domain.User;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        UserSummary user
) {

    public static AuthTokenResponse of(
            final String accessToken,
            final String refreshToken,
            final User user
    ) {
        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                new UserSummary(user.getId(), user.getName().getValue(), user.getProfileImage())
        );
    }

    public record UserSummary(Long id, String nickname, String profileImageUrl) {
    }
}
