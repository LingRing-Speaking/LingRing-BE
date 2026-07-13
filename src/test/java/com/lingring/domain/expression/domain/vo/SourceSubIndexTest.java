package com.lingring.domain.expression.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SourceSubIndexTest {

    @Test
    @DisplayName("shared는 sentinel 값 -1을 가진다")
    void sharedHoldsSentinelValue() {
        // when
        final SourceSubIndex shared = SourceSubIndex.shared();

        // then
        assertThat(shared.getValue()).isEqualTo(-1);
    }

    @Test
    @DisplayName("mistakeIndex는 0 이상의 인덱스를 그대로 담는다")
    void mistakeIndexHoldsGivenIndex() {
        // when
        final SourceSubIndex index = SourceSubIndex.mistakeIndex(0);

        // then
        assertThat(index.getValue()).isZero();
    }

    @Test
    @DisplayName("mistakeIndex에 음수를 주면 InvalidValueException을 던진다")
    void mistakeIndexRejectsNegative() {
        // when & then
        assertThatThrownBy(() -> SourceSubIndex.mistakeIndex(-1))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("같은 값이면 동등하다")
    void equalsByValue() {
        // when & then
        assertThat(SourceSubIndex.mistakeIndex(3)).isEqualTo(SourceSubIndex.mistakeIndex(3));
        assertThat(SourceSubIndex.shared()).isEqualTo(SourceSubIndex.shared());
    }
}
