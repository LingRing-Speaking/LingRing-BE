package com.lingring.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.Level;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.domain.user.dto.response.UserStatsResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserStatsServiceTest extends ServiceIntegrationHelper {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 2);
    private static final Duration TWO_MINUTES = Duration.ofMinutes(2);

    @Autowired
    private UserStatsService userStatsService;

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Nested
    @DisplayName("getByUserId: 사용자 통계 조회")
    class GetByUserId {

        @Test
        @DisplayName("존재하는 userId로 조회하면 초기 상태의 통계를 반환한다")
        void getByUserId_whenExists_returnsResponse() {
            // given
            userStatsRepository.save(UserStats.create(USER_ID));

            // when
            final UserStatsResponse response = userStatsService.getByUserId(USER_ID);

            // then
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.level()).isEqualTo(Level.BEGINNER);
            assertThat(response.mannerTemperature()).isEqualByComparingTo(new BigDecimal("36.5"));
            assertThat(response.totalCallCount()).isZero();
            assertThat(response.currentStreakDays()).isZero();
            assertThat(response.expressionCount()).isZero();
            assertThat(response.lastStudyDate()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 userId로 조회하면 USER_STATS_NOT_FOUND 예외가 발생한다")
        void getByUserId_whenNotFound_throwsNotFoundException() {
            // given
            final Long missingUserId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userStatsService.getByUserId(missingUserId))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_STATS_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("recordCompletedCall: 통화 완료 기록")
    class RecordCompletedCall {

        @Test
        @DisplayName("통화 시간이 1분 미만이면 어떤 필드도 갱신되지 않는다")
        void underOneMinute_doesNotUpdate() {
            // given
            userStatsRepository.save(UserStats.create(USER_ID));

            // when
            userStatsService.recordCompletedCall(USER_ID, Duration.ofSeconds(59), TODAY);

            // then
            final UserStats updated = userStatsRepository.findByUserId(USER_ID).orElseThrow();
            assertThat(updated.getTotalCallCount()).isZero();
            assertThat(updated.getCurrentStreakDays()).isZero();
            assertThat(updated.getLastStudyDate()).isNull();
        }

        @Test
        @DisplayName("통화 시간이 정확히 1분이면 갱신된다 (>= 60초 경계 포함)")
        void exactlyOneMinute_updates() {
            // given
            userStatsRepository.save(UserStats.create(USER_ID));

            // when
            userStatsService.recordCompletedCall(USER_ID, Duration.ofSeconds(60), TODAY);

            // then
            final UserStats updated = userStatsRepository.findByUserId(USER_ID).orElseThrow();
            assertThat(updated.getTotalCallCount()).isEqualTo(1);
            assertThat(updated.getCurrentStreakDays()).isEqualTo(1);
            assertThat(updated.getLastStudyDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("첫 학습이면 count=1, streak=1, lastStudyDate=today로 세팅된다")
        void firstStudy_setsCountAndStreakToOne() {
            // given
            userStatsRepository.save(UserStats.create(USER_ID));

            // when
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY);

            // then
            final UserStats updated = userStatsRepository.findByUserId(USER_ID).orElseThrow();
            assertThat(updated.getTotalCallCount()).isEqualTo(1);
            assertThat(updated.getCurrentStreakDays()).isEqualTo(1);
            assertThat(updated.getLastStudyDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("어제 학습한 상태에서 호출하면 count +1, streak +1, lastStudyDate=today로 갱신된다")
        void yesterdayStudied_incrementsStreak() {
            // given
            userStatsRepository.save(UserStats.create(USER_ID));
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY.minusDays(1));

            // when
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY);

            // then
            final UserStats updated = userStatsRepository.findByUserId(USER_ID).orElseThrow();
            assertThat(updated.getTotalCallCount()).isEqualTo(2);
            assertThat(updated.getCurrentStreakDays()).isEqualTo(2);
            assertThat(updated.getLastStudyDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("오늘 이미 학습한 상태에서 호출하면 count만 +1되고 streak/lastStudyDate는 변하지 않는다")
        void alreadyStudiedToday_incrementsCountOnly() {
            // given
            userStatsRepository.save(UserStats.create(USER_ID));
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY);

            // when
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY);

            // then
            final UserStats updated = userStatsRepository.findByUserId(USER_ID).orElseThrow();
            assertThat(updated.getTotalCallCount()).isEqualTo(2);
            assertThat(updated.getCurrentStreakDays()).isEqualTo(1);
            assertThat(updated.getLastStudyDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("어제 학습 기록이 없는 갭이 있으면 streak가 1로 reset된다")
        void gapResetsStreak() {
            // given: 그제까지만 학습 → 어제는 갭
            userStatsRepository.save(UserStats.create(USER_ID));
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY.minusDays(3));
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY.minusDays(2));

            // when
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY);

            // then
            final UserStats updated = userStatsRepository.findByUserId(USER_ID).orElseThrow();
            assertThat(updated.getTotalCallCount()).isEqualTo(3);
            assertThat(updated.getCurrentStreakDays()).isEqualTo(1);
            assertThat(updated.getLastStudyDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("연속 4일 학습 시나리오: streak가 1→2→3→4로 누적된다")
        void fourConsecutiveDays_accumulatesStreak() {
            // given
            userStatsRepository.save(UserStats.create(USER_ID));

            // when
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY.minusDays(3));
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY.minusDays(2));
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY.minusDays(1));
            userStatsService.recordCompletedCall(USER_ID, TWO_MINUTES, TODAY);

            // then
            final UserStats updated = userStatsRepository.findByUserId(USER_ID).orElseThrow();
            assertThat(updated.getTotalCallCount()).isEqualTo(4);
            assertThat(updated.getCurrentStreakDays()).isEqualTo(4);
            assertThat(updated.getLastStudyDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("존재하지 않는 userId면 USER_STATS_NOT_FOUND 예외가 발생한다 (1분 이상일 때)")
        void recordCompletedCall_whenUserStatsNotFound_throws() {
            // given
            final Long missingUserId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userStatsService.recordCompletedCall(missingUserId, TWO_MINUTES, TODAY))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_STATS_NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 userId라도 1분 미만이면 예외가 발생하지 않는다 (게이트가 먼저 통과)")
        void recordCompletedCall_underOneMinute_doesNotLookupUser() {
            // given
            final Long missingUserId = 9_999_999L;

            // when & then
            userStatsService.recordCompletedCall(missingUserId, Duration.ofSeconds(30), TODAY);
        }
    }
}
