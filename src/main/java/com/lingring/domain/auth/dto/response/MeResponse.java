package com.lingring.domain.auth.dto.response;

import com.lingring.domain.user.domain.User;

public record MeResponse(
        Long id,
        String nickname
) {

    public static MeResponse from(final User user) {
        return new MeResponse(user.getId(), user.getName().getValue());
    }
}