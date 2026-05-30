package com.lingring.domain.user.dto.request;

public record UpdateProfileRequest(
        String nickname,
        String profileImageKey
) {

    public boolean hasNoChanges() {
        return isBlank(nickname) && isBlank(profileImageKey);
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
