package com.lingring.domain.user.domain.vo;

import static lombok.AccessLevel.PROTECTED;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(force = true, access = PROTECTED)
public class ProfileImage {

    private static final int MAX_LENGTH = 500;

    @Column(name = "profile_image", length = MAX_LENGTH)
    private final String value;

    public ProfileImage(@NonNull final String value) {
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "프로필 이미지 URL은 1자 이상 %d자 이하여야 합니다.".formatted(MAX_LENGTH)
            );
        }
        this.value = value;
    }

    public static ProfileImage fromNullable(final String url) {
        if (url == null) {
            return null;
        }
        return new ProfileImage(url);
    }
}
