package com.lingring.global.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageSizeTest {

    @Test
    @DisplayName("요청한 size가 범위 내(1~50)이면 그대로 반환한다")
    void clamp_within_range() {
        assertThat(PageSize.clamp(1).value()).isEqualTo(1);
        assertThat(PageSize.clamp(20).value()).isEqualTo(20);
        assertThat(PageSize.clamp(50).value()).isEqualTo(50);
    }

    @Test
    @DisplayName("요청한 size가 1 미만이면 1로 보정한다")
    void clamp_below_min() {
        assertThat(PageSize.clamp(0).value()).isEqualTo(1);
        assertThat(PageSize.clamp(-10).value()).isEqualTo(1);
    }

    @Test
    @DisplayName("요청한 size가 50 초과면 50으로 보정한다")
    void clamp_above_max() {
        assertThat(PageSize.clamp(51).value()).isEqualTo(50);
        assertThat(PageSize.clamp(1_000_000).value()).isEqualTo(50);
    }
}