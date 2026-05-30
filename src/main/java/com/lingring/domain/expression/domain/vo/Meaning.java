package com.lingring.domain.expression.domain.vo;

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
public class Meaning {

    private static final int MAX_LENGTH = 500;

    @Column(name = "meaning", nullable = false, length = MAX_LENGTH)
    private final String value;

    public Meaning(@NonNull final String value) {
        final String trimmed = value.trim();
        validate(trimmed);
        this.value = trimmed;
    }

    private void validate(final String value) {
        if (value.isEmpty()) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_MEANING,
                    "뜻은 공백일 수 없습니다."
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_MEANING,
                    "뜻은 %d자 이하여야 합니다.".formatted(MAX_LENGTH)
            );
        }
    }
}
