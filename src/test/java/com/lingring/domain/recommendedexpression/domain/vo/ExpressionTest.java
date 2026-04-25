package com.lingring.domain.recommendedexpression.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExpressionTest {

    @Test
    @DisplayName("유효한 추천 표현으로 생성하면 값이 그대로 저장된다")
    void createWithValidValue() {
        final Expression expression = new Expression("How are you?");

        assertThat(expression.getValue()).isEqualTo("How are you?");
    }

    @Test
    @DisplayName("앞뒤 공백은 trim되어 저장된다")
    void trimSurroundingWhitespace() {
        final Expression expression = new Expression("  Hello world  ");

        assertThat(expression.getValue()).isEqualTo("Hello world");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("trim 후 비어있으면 INVALID_RECOMMENDED_EXPRESSION 예외가 발생한다")
    void rejectBlank(final String value) {
        assertThatThrownBy(() -> new Expression(value))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_RECOMMENDED_EXPRESSION);
    }

    @Test
    @DisplayName("trim 후 길이가 500자를 초과하면 INVALID_RECOMMENDED_EXPRESSION 예외가 발생한다")
    void rejectTooLong() {
        final String tooLong = "a".repeat(501);

        assertThatThrownBy(() -> new Expression(tooLong))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_RECOMMENDED_EXPRESSION);
    }

    @Test
    @DisplayName("같은 값을 가진 Expression은 동등하다")
    void equalityByValue() {
        final Expression a = new Expression("Hi");
        final Expression b = new Expression("Hi");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
