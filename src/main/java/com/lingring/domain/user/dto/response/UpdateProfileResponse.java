package com.lingring.domain.user.dto.response;

import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.ProfileImage;

public record UpdateProfileResponse(
        Long id,
        String nickname,
        String profileImage
) {

    public static UpdateProfileResponse from(final User user) {
        return new UpdateProfileResponse(
                user.getId(),
                user.getName().getValue(),
                extractUrl(user.getProfileImage())
        );
    }

    private static String extractUrl(final ProfileImage profileImage) {
        if (profileImage == null) {
            return null;
        }
        return profileImage.getValue();
    }
}
