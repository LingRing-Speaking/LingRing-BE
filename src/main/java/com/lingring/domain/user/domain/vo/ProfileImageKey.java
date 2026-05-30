package com.lingring.domain.user.domain.vo;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.ForbiddenException;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode
public class ProfileImageKey {

    private static final String PREFIX = "profile-images/";

    private final String value;

    private ProfileImageKey(final String value) {
        this.value = value;
    }

    public static ProfileImageKey from(@NonNull final String value) {
        return new ProfileImageKey(value);
    }

    public static ProfileImageKey generateFor(@NonNull final Long userId) {
        return new ProfileImageKey(PREFIX + userId + "/" + UUID.randomUUID());
    }

    public void requireOwnedBy(@NonNull final Long userId) {
        final String requiredPrefix = PREFIX + userId + "/";
        if (!value.startsWith(requiredPrefix)) {
            throw new ForbiddenException(
                    ErrorCode.PROFILE_IMAGE_KEY_FORBIDDEN,
                    "본인의 프로필 이미지 키만 사용할 수 있습니다: %s".formatted(value)
            );
        }
    }
}
