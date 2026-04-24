package com.lingring.domain.user.dto.response;

import com.lingring.domain.user.domain.Level;
import com.lingring.domain.user.domain.UserStats;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UserStatsResponse(
        Long userId,
        Level level,
        BigDecimal mannerTemperature,
        int totalCallCount,
        int currentStreakDays,
        int savedExpressionCount,
        LocalDate lastStudyDate
) {

    public static UserStatsResponse from(final UserStats userStats) {
        return new UserStatsResponse(
                userStats.getUserId(),
                userStats.getLevel(),
                userStats.getMannerTemperature(),
                userStats.getTotalCallCount(),
                userStats.getCurrentStreakDays(),
                userStats.getSavedExpressionCount(),
                userStats.getLastStudyDate()
        );
    }
}
