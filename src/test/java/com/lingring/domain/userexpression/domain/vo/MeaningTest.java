package com.lingring.domain.savedexpression.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MeaningTest {

    @Test
    @DisplayName("유효한 뜻으로 생성하면 값이 그대로 저장된다")
    void createWithValidValue() {
        final Meaning meaning = new Meaning("안녕하세요");

        assertThat(meaning.getValue()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("앞뒤 공백은 trim되어 저장된다")
    void trimSurroundingWhitespace() {
        final Meaning meaning = new Meaning("  안녕  ");

        assertThat(meaning.getValue()).isEqualTo("안녕");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("trim 후 비어있으면 INVALID_MEANING 예외가 발생한다")
    void rejectBlank(final String value) {
        assertThatThrownBy(() -> new Meaning(value))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_MEANING);
    }

    @Test
    @DisplayName("trim 후 길이가 500자를 초과하면 INVALID_MEANING 예외가 발생한다")
    void rejectTooLong() {
        final String tooLong = "가".repeat(501);

        assertThatThrownBy(() -> new Meaning(tooLong))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_MEANING);
    }
}
