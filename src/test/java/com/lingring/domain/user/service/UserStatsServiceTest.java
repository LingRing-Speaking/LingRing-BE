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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserStatsServiceTest extends ServiceIntegrationHelper {

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
            final Long userId = 1L;
            userStatsRepository.save(UserStats.create(userId));

            // when
            final UserStatsResponse response = userStatsService.getByUserId(userId);

            // then
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.level()).isEqualTo(Level.BEGINNER);
            assertThat(response.mannerTemperature()).isEqualByComparingTo(new BigDecimal("36.5"));
            assertThat(response.totalCallCount()).isZero();
            assertThat(response.currentStreakDays()).isZero();
            assertThat(response.savedExpressionCount()).isZero();
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
}
