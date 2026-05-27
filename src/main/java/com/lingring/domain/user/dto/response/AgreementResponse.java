package com.lingring.domain.user.dto.response;

import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.ProfileImage;

public record AgreementResponse(
        UserSummary user
) {

    public static AgreementResponse from(final User user) {
        return new AgreementResponse(
                new UserSummary(
                        user.getId(),
                        user.getName().getValue(),
                        extractUrl(user.getProfileImage()),
                        user.requiresOnboarding()
                )
        );
    }

    private static String extractUrl(final ProfileImage profileImage) {
        if (profileImage == null) {
            return null;
        }
        return profileImage.getValue();
    }

    public record UserSummary(
            Long id,
            String nickname,
            String profileImage,
            boolean requiresOnboarding
    ) {
    }
}
