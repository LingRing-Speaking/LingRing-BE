package com.lingring.domain.recommendedexpression.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecommendedExpressionTest {

    @Test
    @DisplayName("create는 VO로 래핑된 expression/meaning을 가진 엔티티를 생성한다")
    void createWrapsValuesIntoEntity() {
        // given
        final String expression = "How are you?";
        final String meaning = "어떻게 지내세요?";

        // when
        final RecommendedExpression recommendedExpression =
                RecommendedExpression.create(expression, meaning);

        // then
        assertThat(recommendedExpression.getExpression().getValue()).isEqualTo(expression);
        assertThat(recommendedExpression.getMeaning().getValue()).isEqualTo(meaning);
    }

    @Test
    @DisplayName("create 호출 시 expression VO 검증이 실행된다")
    void createValidatesExpression() {
        assertThatThrownBy(() -> RecommendedExpression.create("   ", "안녕"))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_RECOMMENDED_EXPRESSION);
    }

    @Test
    @DisplayName("create 호출 시 meaning VO 검증이 실행된다")
    void createValidatesMeaning() {
        assertThatThrownBy(() -> RecommendedExpression.create("Hello", "   "))
                .isInstanceOf(InvalidValueException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_RECOMMENDED_MEANING);
    }
}
