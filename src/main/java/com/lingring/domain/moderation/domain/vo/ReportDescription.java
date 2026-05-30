package com.lingring.domain.moderation.domain.vo;

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
public class ReportDescription {

    private static final int MIN_LENGTH = 5;
    private static final int MAX_LENGTH = 200;

    @Column(name = "description", nullable = false, length = MAX_LENGTH)
    private final String value;

    public ReportDescription(@NonNull final String value) {
        final String trimmed = value.trim();
        validate(trimmed);
        this.value = trimmed;
    }

    private void validate(final String value) {
        if (value.length() < MIN_LENGTH) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_REPORT_DESCRIPTION,
                    "신고 상세 내용은 %d자 이상이어야 합니다.".formatted(MIN_LENGTH)
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_REPORT_DESCRIPTION,
                    "신고 상세 내용은 %d자 이하여야 합니다.".formatted(MAX_LENGTH)
            );
        }
    }
}
