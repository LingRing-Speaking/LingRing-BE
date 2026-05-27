package com.lingring.domain.callanalysis.domain.analysis.vo;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;

public record MistakeItem(
        FeedbackTag tag,
        String wrong,
        String improved,
        String reason,
        String koMeaning
) {

    public MistakeItem {
        requireNonBlank(wrong, "wrong");
        requireNonBlank(improved, "improved");
        requireNonBlank(reason, "reason");
        requireNonBlank(koMeaning, "koMeaning");
        if (tag == null) {
            throw new InvalidValueException(ErrorCode.INVALID_INPUT_VALUE, "tag는 필수입니다.");
        }
    }

    private static void requireNonBlank(final String value, final String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "%s는 빈 값일 수 없습니다.".formatted(field)
            );
        }
    }
}
