package com.lingring.domain.review.domain.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.review.exception.AnalysisQuotaExhaustedException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnalysisQuotaTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 28);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    @Nested
    @DisplayName("consumeOn: 무료 → 유료 → 소진 순서로 차감 (aggregate 정책)")
    class ConsumeOn {

        @Test
        @DisplayName("무료가 남아 있으면 무료에서 차감하고 유료는 건드리지 않는다")
        void consumeOn_whenFreeAvailable_consumesFreeNotPaid() {
            // given
            final AnalysisQuota quota = AnalysisQuota.initial(USER_ID);
            quota.charge(2);

            // when
            quota.consumeOn(TODAY);

            // then
            assertThat(quota.freeRemainingOn(TODAY)).isZero();
            assertThat(quota.paidRemaining()).isEqualTo(2);
        }

        @Test
        @DisplayName("같은 날 무료를 모두 쓰고 유료가 0이면 예외를 던진다")
        void consumeOn_whenFreeExhaustedAndNoPaid_throws() {
            // given
            final AnalysisQuota quota = AnalysisQuota.initial(USER_ID);
            quota.consumeOn(TODAY);

            // when & then
            assertThatThrownBy(() -> quota.consumeOn(TODAY))
                    .isInstanceOf(AnalysisQuotaExhaustedException.class);
        }

        @Test
        @DisplayName("무료 소진 후 유료가 있으면 유료에서 차감한다")
        void consumeOn_whenFreeExhaustedAndPaidAvailable_consumesPaid() {
            // given
            final AnalysisQuota quota = AnalysisQuota.initial(USER_ID);
            quota.charge(2);
            quota.consumeOn(TODAY);

            // when
            quota.consumeOn(TODAY);

            // then
            assertThat(quota.freeRemainingOn(TODAY)).isZero();
            assertThat(quota.paidRemaining()).isEqualTo(1);
        }

        @Test
        @DisplayName("무료·유료 모두 소진되면 예외를 던진다")
        void consumeOn_whenBothExhausted_throws() {
            // given
            final AnalysisQuota quota = AnalysisQuota.initial(USER_ID);
            quota.charge(1);
            quota.consumeOn(TODAY);
            quota.consumeOn(TODAY);

            // when & then
            assertThatThrownBy(() -> quota.consumeOn(TODAY))
                    .isInstanceOf(AnalysisQuotaExhaustedException.class);
        }

        @Test
        @DisplayName("날이 바뀌면 무료가 리필되어 다시 차감할 수 있다")
        void consumeOn_whenNewDay_refillsFree() {
            // given
            final AnalysisQuota quota = AnalysisQuota.initial(USER_ID);
            quota.consumeOn(TODAY);

            // when
            quota.consumeOn(TOMORROW);

            // then
            assertThat(quota.freeResetDate()).isEqualTo(TOMORROW);
            assertThat(quota.freeRemainingOn(TOMORROW)).isZero();
            assertThat(quota.paidRemaining()).isZero();
        }
    }

    @Nested
    @DisplayName("조회/충전 위임")
    class Delegation {

        @Test
        @DisplayName("초기 상태는 무료 한도 1, 유료 0이다")
        void initial_hasFreeLimitAndNoPaid() {
            // given
            final AnalysisQuota quota = AnalysisQuota.initial(USER_ID);

            // when & then
            assertThat(quota.freeRemainingOn(TODAY)).isEqualTo(1);
            assertThat(quota.paidRemaining()).isZero();
        }

        @Test
        @DisplayName("charge 하면 유료 잔여가 증가한다")
        void charge_increasesPaidRemaining() {
            // given
            final AnalysisQuota quota = AnalysisQuota.initial(USER_ID);

            // when
            quota.charge(3);

            // then
            assertThat(quota.paidRemaining()).isEqualTo(3);
        }
    }
}
