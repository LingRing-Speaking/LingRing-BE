package com.lingring.domain.savedexpression.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SavedExpressionTest {

    @Test
    @DisplayName("create는 userId와 VO로 래핑된 expression/meaning을 가진 엔티티를 생성한다")
    void createWrapsValuesIntoEntity() {
        // given
        final Long userId = 1L;
        final String expression = "How are you?";
        final String meaning = "어떻게 지내세요?";

        // when
        final SavedExpression savedExpression = SavedExpression.create(userId, expression, meaning);

        // then
        assertThat(savedExpression.getUserId()).isEqualTo(userId);
        assertThat(savedExpression.getExpression().getValue()).isEqualTo(expression);
        assertThat(savedExpression.getMeaning().getValue()).isEqualTo(meaning);
    }
}
