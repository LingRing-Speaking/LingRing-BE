package com.lingring.domain.user.dto.response;

import com.lingring.domain.user.domain.User;

public record AgreementResponse(
        UserSummaryResponse user
) {

    public static AgreementResponse from(final User user) {
        return new AgreementResponse(UserSummaryResponse.from(user));
    }
}
