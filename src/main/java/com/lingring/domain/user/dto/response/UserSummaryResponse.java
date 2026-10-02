package com.lingring.domain.user.dto.response;

import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.MarketingPushConsent;
import com.lingring.domain.user.domain.vo.ProfileImage;
import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long id,
        String nickname,
        String profileImage,
        boolean requiresOnboarding,
        String agreedTermsVersion,
        boolean marketingPushAgreed,
        LocalDateTime marketingPushUpdatedAt
) {

    public static UserSummaryResponse from(final User user) {
        final MarketingPushConsent consent = user.getMarketingPushConsent();
        return new UserSummaryResponse(
                user.getId(),
                user.getName().getValue(),
                extractUrl(user.getProfileImage()),
                user.requiresOnboarding(),
                user.agreedTermsVersion(),
                consent.isAgreed(),
                consent.getUpdatedAt()
        );
    }

    private static String extractUrl(final ProfileImage profileImage) {
        if (profileImage == null) {
            return null;
        }
        return profileImage.getValue();
    }
}
