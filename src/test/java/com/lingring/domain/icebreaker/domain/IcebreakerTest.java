package com.lingring.domain.icebreaker.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IcebreakerTest {

    @Test
    @DisplayName("create는 VO로 래핑된 expression/meaning을 가진 엔티티를 생성한다")
    void createWrapsValuesIntoEntity() {
        // given
        final String expression = "Hi, how was your day?";
        final String meaning = "오늘 하루 어땠어?";

        // when
        final Icebreaker icebreaker = Icebreaker.create(expression, meaning);

        // then
        assertThat(icebreaker.getExpression().getValue()).isEqualTo(expression);
        assertThat(icebreaker.getMeaning().getValue()).isEqualTo(meaning);
    }

    @Test
    @DisplayName("create 호출 시 expression VO 검증이 실행된다")
    void createValidatesExpression() {
        assertThatThrownBy(() -> Icebreaker.create("   ", "안녕"))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ICEBREAKER_EXPRESSION);
    }

    @Test
    @DisplayName("create 호출 시 meaning VO 검증이 실행된다")
    void createValidatesMeaning() {
        assertThatThrownBy(() -> Icebreaker.create("Hello", "   "))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ICEBREAKER_MEANING);
    }
}
