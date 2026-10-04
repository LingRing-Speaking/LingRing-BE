package com.lingring.domain.user.dto.response;

import com.lingring.domain.user.domain.User;

public record NotificationSettingResponse(
        UserSummaryResponse user
) {

    public static NotificationSettingResponse from(final User user) {
        return new NotificationSettingResponse(UserSummaryResponse.from(user));
    }
}
