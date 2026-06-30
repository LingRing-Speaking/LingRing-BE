package com.lingring.domain.user.dto.response;

import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.ProfileImage;

public record MeResponse(
        Long id,
        String nickname,
        String profileImage,
        boolean requiresOnboarding,
        String agreedTermsVersion
) {

    public static MeResponse from(final User user) {
        return new MeResponse(
                user.getId(),
                user.getName().getValue(),
                extractUrl(user.getProfileImage()),
                user.requiresOnboarding(),
                user.agreedTermsVersion()
        );
    }

    private static String extractUrl(final ProfileImage profileImage) {
        if (profileImage == null) {
            return null;
        }
        return profileImage.getValue();
    }
}
