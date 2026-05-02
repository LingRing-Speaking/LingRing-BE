package com.lingring.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserStatsTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 2);

    @Test
    @DisplayName("create는 userId를 보관하고 초기 상태(BEGINNER, 매너 온도 36.5, 카운트 0, 마지막 학습일 null)로 생성한다")
    void createSetsInitialState() {
        // when
        final UserStats userStats = UserStats.create(USER_ID);

        // then
        assertThat(userStats.getUserId()).isEqualTo(USER_ID);
        assertThat(userStats.getLevel()).isEqualTo(Level.BEGINNER);
        assertThat(userStats.getMannerTemperature()).isEqualByComparingTo(new BigDecimal("36.5"));
        assertThat(userStats.getTotalCallCount()).isZero();
        assertThat(userStats.getCurrentStreakDays()).isZero();
        assertThat(userStats.getExpressionCount()).isZero();
        assertThat(userStats.getLastStudyDate()).isNull();
    }

    @Nested
    @DisplayName("hasStudiedOn: 특정 날짜에 학습했는지")
    class HasStudiedOn {

        @Test
        @DisplayName("lastStudyDate가 null이면 어떤 날짜에 대해서도 false를 반환한다")
        void whenLastStudyDateNull_returnsFalse() {
            // given
            final UserStats userStats = UserStats.create(USER_ID);

            // when & then
            assertThat(userStats.hasStudiedOn(TODAY)).isFalse();
        }

        @Test
        @DisplayName("lastStudyDate가 동일한 날짜면 true를 반환한다")
        void whenLastStudyDateEqualsDate_returnsTrue() {
            // given
            final UserStats userStats = UserStats.create(USER_ID);
            userStats.updateLastStudyDate(TODAY);

            // when & then
            assertThat(userStats.hasStudiedOn(TODAY)).isTrue();
        }

        @Test
        @DisplayName("lastStudyDate가 다른 날짜면 false를 반환한다")
        void whenLastStudyDateDiffers_returnsFalse() {
            // given
            final UserStats userStats = UserStats.create(USER_ID);
            userStats.updateLastStudyDate(TODAY.minusDays(1));

            // when & then
            assertThat(userStats.hasStudiedOn(TODAY)).isFalse();
        }
    }

    @Nested
    @DisplayName("isContinuingStreakOn: today가 streak를 이어가는 날인지")
    class IsContinuingStreakOn {

        @Test
        @DisplayName("lastStudyDate가 null이면 false를 반환한다")
        void whenLastStudyDateNull_returnsFalse() {
            // given
            final UserStats userStats = UserStats.create(USER_ID);

            // when & then
            assertThat(userStats.isContinuingStreakOn(TODAY)).isFalse();
        }

        @Test
        @DisplayName("lastStudyDate가 어제면 true를 반환한다")
        void whenLastStudyDateIsYesterday_returnsTrue() {
            // given
            final UserStats userStats = UserStats.create(USER_ID);
            userStats.updateLastStudyDate(TODAY.minusDays(1));

            // when & then
            assertThat(userStats.isContinuingStreakOn(TODAY)).isTrue();
        }

        @Test
        @DisplayName("lastStudyDate가 오늘이면 false를 반환한다 (연속이 아니라 동일 날짜)")
        void whenLastStudyDateIsToday_returnsFalse() {
            // given
            final UserStats userStats = UserStats.create(USER_ID);
            userStats.updateLastStudyDate(TODAY);

            // when & then
            assertThat(userStats.isContinuingStreakOn(TODAY)).isFalse();
        }

        @Test
        @DisplayName("lastStudyDate가 그제 이전이면(갭) false를 반환한다")
        void whenLastStudyDateHasGap_returnsFalse() {
            // given
            final UserStats userStats = UserStats.create(USER_ID);
            userStats.updateLastStudyDate(TODAY.minusDays(2));

            // when & then
            assertThat(userStats.isContinuingStreakOn(TODAY)).isFalse();
        }
    }
}
