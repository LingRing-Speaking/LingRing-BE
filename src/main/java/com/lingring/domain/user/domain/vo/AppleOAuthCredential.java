package com.lingring.domain.user.domain.vo;

import static lombok.AccessLevel.PROTECTED;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import com.lingring.global.security.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(force = true, access = PROTECTED)
public class AppleOAuthCredential {

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "apple_refresh_token", length = 1024)
    private final String refreshToken;

    public AppleOAuthCredential(@NonNull final String refreshToken) {
        validate(refreshToken);
        this.refreshToken = refreshToken;
    }

    public static AppleOAuthCredential of(@NonNull final String refreshToken) {
        return new AppleOAuthCredential(refreshToken);
    }

    private void validate(final String refreshToken) {
        if (refreshToken.isBlank()) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "Apple refresh_token은 비어있을 수 없습니다."
            );
        }
    }
}
