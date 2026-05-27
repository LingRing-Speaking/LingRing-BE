package com.lingring.domain.moderation.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReportDescriptionTest {

    @Test
    @DisplayName("유효한 입력은 trim된 값을 보관한다")
    void createWithValidValue() {
        final ReportDescription description = new ReportDescription("  도배성 메시지를 반복적으로 보냄  ");

        assertThat(description.getValue()).isEqualTo("도배성 메시지를 반복적으로 보냄");
    }

    @Test
    @DisplayName("정확히 5자면 통과한다 (경계값)")
    void acceptMinLength() {
        final ReportDescription description = new ReportDescription("ABCDE");

        assertThat(description.getValue()).hasSize(5);
    }

    @Test
    @DisplayName("정확히 200자면 통과한다 (경계값)")
    void acceptMaxLength() {
        final String value = "a".repeat(200);

        final ReportDescription description = new ReportDescription(value);

        assertThat(description.getValue()).hasSize(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "1234", "    "})
    @DisplayName("trim 후 5자 미만이면 INVALID_REPORT_DESCRIPTION 예외가 발생한다")
    void rejectTooShort(final String value) {
        assertThatThrownBy(() -> new ReportDescription(value))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REPORT_DESCRIPTION);
    }

    @Test
    @DisplayName("trim 후 200자를 초과하면 INVALID_REPORT_DESCRIPTION 예외가 발생한다")
    void rejectTooLong() {
        final String tooLong = "a".repeat(201);

        assertThatThrownBy(() -> new ReportDescription(tooLong))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REPORT_DESCRIPTION);
    }
}
