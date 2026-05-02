package com.lingring.domain.withdrawallog.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WithdrawDescriptionTest {

    @Test
    @DisplayName("유효한 입력은 trim된 값을 보관한다")
    void createWithValidValue() {
        // given
        final String raw = "  더 이상 사용할 일이 없어요  ";

        // when
        final WithdrawDescription description = new WithdrawDescription(raw);

        // then
        assertThat(description.getValue()).isEqualTo("더 이상 사용할 일이 없어요");
    }

    @Test
    @DisplayName("정확히 200자면 통과한다 (경계값)")
    void acceptMaxLength() {
        // given
        final String value = "a".repeat(200);

        // when
        final WithdrawDescription description = new WithdrawDescription(value);

        // then
        assertThat(description.getValue()).hasSize(200);
    }

    @Test
    @DisplayName("trim 후 200자를 초과하면 INVALID_WITHDRAW_DESCRIPTION 예외가 발생한다")
    void rejectTooLong() {
        // given
        final String tooLong = "a".repeat(201);

        // when & then
        assertThatThrownBy(() -> new WithdrawDescription(tooLong))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_WITHDRAW_DESCRIPTION);
    }

    @Test
    @DisplayName("null 입력은 NPE를 발생시킨다 (@NonNull 가드)")
    void rejectNull() {
        // when & then
        assertThatThrownBy(() -> new WithdrawDescription(null))
                .isInstanceOf(NullPointerException.class);
    }
}
