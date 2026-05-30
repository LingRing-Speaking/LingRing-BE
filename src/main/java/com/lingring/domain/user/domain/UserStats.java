package com.lingring.domain.user.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "user_stats")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class UserStats extends BaseTimeEntity {

    private static final BigDecimal INITIAL_MANNER_TEMPERATURE = new BigDecimal("36.5");

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(STRING)
    @Column(name = "level", nullable = false)
    private Level level;

    @Column(name = "manner_temperature", nullable = false, precision = 4, scale = 1)
    private BigDecimal mannerTemperature;

    @Column(name = "total_call_count", nullable = false)
    private int totalCallCount;

    @Column(name = "current_streak_days", nullable = false)
    private int currentStreakDays;

    @Column(name = "expression_count", nullable = false)
    private int expressionCount;

    @Column(name = "last_study_date")
    private LocalDate lastStudyDate;

    private UserStats(@NonNull final Long userId) {
        this.userId = userId;
        this.level = Level.BEGINNER;
        this.mannerTemperature = INITIAL_MANNER_TEMPERATURE;
        this.totalCallCount = 0;
        this.currentStreakDays = 0;
        this.expressionCount = 0;
        this.lastStudyDate = null;
    }

    public static UserStats create(@NonNull final Long userId) {
        return new UserStats(userId);
    }

    public void increaseTotalCallCount() {
        this.totalCallCount += 1;
    }

    public void increaseStreakDays() {
        this.currentStreakDays += 1;
    }

    public void resetStreakDaysToOne() {
        this.currentStreakDays = 1;
    }

    public void updateLastStudyDate(@NonNull final LocalDate date) {
        this.lastStudyDate = date;
    }

    public boolean hasStudiedOn(@NonNull final LocalDate date) {
        return date.equals(lastStudyDate);
    }

    public boolean isContinuingStreakOn(@NonNull final LocalDate today) {
        return lastStudyDate != null && lastStudyDate.equals(today.minusDays(1));
    }
}
