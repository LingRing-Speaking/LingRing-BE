package com.lingring.domain.auth.dto.response;

import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.ProfileImage;

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
                new UserSummary(user.getId(), user.getName().getValue(), extractUrl(user.getProfileImage()))
        );
    }

    private static String extractUrl(final ProfileImage profileImage) {
        if (profileImage == null) {
            return null;
        }
        return profileImage.getValue();
    }

    public record UserSummary(Long id, String nickname, String profileImageUrl) {
    }
}
