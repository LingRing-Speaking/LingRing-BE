package com.lingring.domain.user.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NameTest {

    @Test
    @DisplayName("유효한 이름으로 생성하면 값이 그대로 저장된다")
    void createWithValidValue() {
        final Name name = new Name("링링");

        assertThat(name.getValue()).isEqualTo("링링");
    }

    @Test
    @DisplayName("앞뒤 공백은 trim되어 저장된다")
    void trimSurroundingWhitespace() {
        final Name name = new Name("  링링  ");

        assertThat(name.getValue()).isEqualTo("링링");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", " ", "", "   "})
    @DisplayName("trim 후 길이가 2자 미만이면 예외가 발생한다")
    void rejectTooShort(final String value) {
        assertThatThrownBy(() -> new Name(value))
            .isInstanceOf(InvalidValueException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_USER_NAME);
    }

    @Test
    @DisplayName("trim 후 길이가 30자를 초과하면 예외가 발생한다")
    void rejectTooLong() {
        final String tooLong = "a".repeat(31);

        assertThatThrownBy(() -> new Name(tooLong))
            .isInstanceOf(InvalidValueException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_USER_NAME);
    }

    @Test
    @DisplayName("최대 길이(30자) 이름은 생성된다")
    void acceptMaxLength() {
        final String thirtyChars = "a".repeat(30);

        final Name name = new Name(thirtyChars);

        assertThat(name.getValue()).isEqualTo(thirtyChars);
    }

    @Test
    @DisplayName("FE 랜덤 닉네임 형식(예: funny-otter-7891, 16자)이 허용된다")
    void acceptFeNicknameFormat() {
        final Name name = new Name("funny-otter-7891");

        assertThat(name.getValue()).isEqualTo("funny-otter-7891");
    }

    @Test
    @DisplayName("같은 값을 가진 Name은 동등하다")
    void equalityByValue() {
        final Name a = new Name("링링");
        final Name b = new Name("링링");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
