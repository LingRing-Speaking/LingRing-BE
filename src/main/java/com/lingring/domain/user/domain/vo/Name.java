package com.lingring.domain.user.domain.vo;

import static lombok.AccessLevel.PROTECTED;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(force = true, access = PROTECTED)
public class Name {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 12;

    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^[가-힣a-zA-Z0-9]+$");

    @Column(name = "name", nullable = false, length = MAX_LENGTH)
    private final String value;

    public Name(@NonNull final String value) {
        final String trimmed = value.trim();
        validate(trimmed);
        this.value = trimmed;
    }

    private void validate(final String value) {
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                ErrorCode.INVALID_USER_NAME,
                "이름은 %d자 이상 %d자 이하여야 합니다.".formatted(MIN_LENGTH, MAX_LENGTH)
            );
        }
        if (!ALLOWED_CHARACTERS.matcher(value).matches()) {
            throw new InvalidValueException(
                ErrorCode.INVALID_USER_NAME,
                "이름은 한글·영문·숫자만 사용할 수 있습니다: %s".formatted(value)
            );
        }
    }
}
