package com.lingring.domain.push.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DailyReminderPolicyTest {

    private static LocalDateTime at(final int hour, final int minute, final int second) {
        return LocalDateTime.of(2026, 10, 2, hour, minute, second);
    }

    @Nested
    @DisplayName("isSendable: 20:00 이상 21:00 미만에만 발송 가능")
    class IsSendable {

        @Test
        @DisplayName("20:00 정각이면 발송 가능")
        void isSendable_atStart_returnsTrue() {
            // when & then
            assertThat(DailyReminderPolicy.isSendable(at(20, 0, 0))).isTrue();
        }

        @Test
        @DisplayName("20:59:59이면 발송 가능")
        void isSendable_justBeforeDeadline_returnsTrue() {
            // when & then
            assertThat(DailyReminderPolicy.isSendable(at(20, 59, 59))).isTrue();
        }

        @Test
        @DisplayName("21:00 정각이면 발송 불가 (야간 광고성 정보 제한)")
        void isSendable_atDeadline_returnsFalse() {
            // when & then
            assertThat(DailyReminderPolicy.isSendable(at(21, 0, 0))).isFalse();
        }

        @Test
        @DisplayName("19:59:59이면 발송 불가")
        void isSendable_beforeStart_returnsFalse() {
            // when & then
            assertThat(DailyReminderPolicy.isSendable(at(19, 59, 59))).isFalse();
        }
    }
}
