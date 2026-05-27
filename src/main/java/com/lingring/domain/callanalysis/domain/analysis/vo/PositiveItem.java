package com.lingring.domain.callanalysis.domain.analysis.vo;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;

public record PositiveItem(
        String sentence,
        String goodPart,
        String koMeaning
) {

    public PositiveItem {
        requireNonBlank(sentence, "sentence");
        requireNonBlank(goodPart, "goodPart");
        requireNonBlank(koMeaning, "koMeaning");
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
