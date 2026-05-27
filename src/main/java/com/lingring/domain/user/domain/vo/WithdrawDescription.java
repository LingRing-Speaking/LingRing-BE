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
public class WithdrawDescription {

    private static final int MAX_LENGTH = 200;

    @Column(name = "description", length = MAX_LENGTH)
    private final String value;

    public WithdrawDescription(@NonNull final String value) {
        final String trimmed = value.trim();
        validate(trimmed);
        this.value = trimmed;
    }

    private void validate(final String value) {
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_WITHDRAW_DESCRIPTION,
                    "탈퇴 사유 상세 내용은 %d자 이하여야 합니다.".formatted(MAX_LENGTH)
            );
        }
    }
}
