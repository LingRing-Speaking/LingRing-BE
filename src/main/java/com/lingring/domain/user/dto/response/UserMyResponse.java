package com.lingring.domain.user.dto.response;

import com.lingring.domain.user.domain.User;

public record UserMyResponse(
        Long id,
        String name
) {

    public static UserMyResponse from(final User user) {
        return new UserMyResponse(user.getId(), user.getName().getValue());
    }
}
