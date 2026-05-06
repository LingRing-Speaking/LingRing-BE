package com.lingring.domain.user.domain.vo;

import static lombok.AccessLevel.PROTECTED;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(force = true, access = PROTECTED)
public class Agreement {

    private static final int MAX_VERSION_LENGTH = 20;

    @Column(name = "agreed_terms_version", length = MAX_VERSION_LENGTH)
    private final String termsVersion;

    @Column(name = "agreed_at")
    private final LocalDateTime agreedAt;

    public Agreement(@NonNull final String termsVersion, @NonNull final LocalDateTime agreedAt) {
        validate(termsVersion);
        this.termsVersion = termsVersion;
        this.agreedAt = agreedAt;
    }

    public static Agreement of(@NonNull final String termsVersion, @NonNull final LocalDateTime agreedAt) {
        return new Agreement(termsVersion, agreedAt);
    }

    private void validate(final String termsVersion) {
        if (termsVersion.isBlank() || termsVersion.length() > MAX_VERSION_LENGTH) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "약관 버전은 1자 이상 %d자 이하여야 합니다.".formatted(MAX_VERSION_LENGTH)
            );
        }
    }
}