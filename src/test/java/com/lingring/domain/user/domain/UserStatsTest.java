package com.lingring.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserStatsTest {

    @Test
    @DisplayName("create는 userId를 보관하고 초기 상태(BEGINNER, 매너 온도 36.5, 카운트 0, 마지막 학습일 null)로 생성한다")
    void createSetsInitialState() {
        // given
        final Long userId = 1L;

        // when
        final UserStats userStats = UserStats.create(userId);

        // then
        assertThat(userStats.getUserId()).isEqualTo(userId);
        assertThat(userStats.getLevel()).isEqualTo(Level.BEGINNER);
        assertThat(userStats.getMannerTemperature()).isEqualByComparingTo(new BigDecimal("36.5"));
        assertThat(userStats.getTotalCallCount()).isZero();
        assertThat(userStats.getCurrentStreakDays()).isZero();
        assertThat(userStats.getExpressionCount()).isZero();
        assertThat(userStats.getLastStudyDate()).isNull();
    }
}
