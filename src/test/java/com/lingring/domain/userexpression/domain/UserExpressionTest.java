package com.lingring.domain.userexpression.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserExpressionTest {

    @Test
    @DisplayName("create는 userId와 VO로 래핑된 expression/meaning을 가진 엔티티를 생성한다")
    void createWrapsValuesIntoEntity() {
        // given
        final Long userId = 1L;
        final String expression = "How are you?";
        final String meaning = "어떻게 지내세요?";

        // when
        final UserExpression userExpression = UserExpression.create(userId, expression, meaning);

        // then
        assertThat(userExpression.getUserId()).isEqualTo(userId);
        assertThat(userExpression.getExpression().getValue()).isEqualTo(expression);
        assertThat(userExpression.getMeaning().getValue()).isEqualTo(meaning);
    }
}
