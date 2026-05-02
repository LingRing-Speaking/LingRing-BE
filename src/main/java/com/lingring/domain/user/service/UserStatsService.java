package com.lingring.domain.user.service;

import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.domain.user.dto.response.UserStatsResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.time.Duration;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private static final Duration MIN_QUALIFYING_DURATION = Duration.ofMinutes(1);

    private final UserStatsRepository userStatsRepository;

    @Transactional(readOnly = true)
    public UserStatsResponse getByUserId(final Long userId) {
        final UserStats userStats = findByUserId(userId);
        return UserStatsResponse.from(userStats);
    }

    @Transactional
    public void recordCompletedCall(
            final Long userId,
            final Duration callDuration,
            final LocalDate today
    ) {
        if (callDuration.compareTo(MIN_QUALIFYING_DURATION) < 0) {
            return;
        }
        final UserStats userStats = findByUserId(userId);

        userStats.increaseTotalCallCount();
        if (userStats.hasStudiedOn(today)) {
            return;
        }
        advanceStreak(userStats, today);
        userStats.updateLastStudyDate(today);
    }

    private void advanceStreak(final UserStats userStats, final LocalDate today) {
        if (userStats.isContinuingStreakOn(today)) {
            userStats.increaseStreakDays();
            return;
        }
        userStats.resetStreakDaysToOne();
    }

    private UserStats findByUserId(final Long userId) {
        return userStatsRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_STATS_NOT_FOUND,
                        "userId가 %d인 사용자 통계를 찾을 수 없습니다.".formatted(userId)
                ));
    }
}
