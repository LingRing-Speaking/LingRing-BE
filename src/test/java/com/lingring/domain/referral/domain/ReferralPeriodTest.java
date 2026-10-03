package com.lingring.domain.referral.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReferralPeriodTest {

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 10, 3, 14, 0);

    @Test
    @DisplayName("입력 마감 시각은 가입 시각 + 7일이다")
    void startingAt_setsUntilSevenDaysAfterJoin() {
        // when
        final ReferralPeriod period = ReferralPeriod.startingAt(JOINED_AT);

        // then
        assertThat(period.getUntil()).isEqualTo(LocalDateTime.of(2026, 10, 10, 14, 0));
    }

    @Test
    @DisplayName("마감 직전에는 입력할 수 있다")
    void isOpenAt_whenJustBeforeUntil_returnsTrue() {
        // given
        final ReferralPeriod period = ReferralPeriod.startingAt(JOINED_AT);

        // when & then
        assertThat(period.isOpenAt(period.getUntil().minusNanos(1_000))).isTrue();
    }

    @Test
    @DisplayName("마감 시각과 같거나 지나면 입력할 수 없다")
    void isOpenAt_whenAtOrAfterUntil_returnsFalse() {
        // given
        final ReferralPeriod period = ReferralPeriod.startingAt(JOINED_AT);

        // when & then
        assertThat(period.isOpenAt(period.getUntil())).isFalse();
        assertThat(period.isOpenAt(period.getUntil().plusSeconds(1))).isFalse();
    }
}
