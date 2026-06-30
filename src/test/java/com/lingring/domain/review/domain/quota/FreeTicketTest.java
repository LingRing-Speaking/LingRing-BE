package com.lingring.domain.review.domain.quota;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FreeTicketTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 28);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    @Test
    @DisplayName("초기 무료 티켓은 오늘 기준 한도(1)만큼 남아 있다")
    void remainingOn_whenInitial_returnsDailyLimit() {
        // given
        final FreeTicket ticket = FreeTicket.initial();

        // when & then
        assertThat(ticket.remainingOn(TODAY)).isEqualTo(1);
    }

    @Test
    @DisplayName("오늘 소비하면 잔여가 0이 되고 true를 반환한다")
    void consumeOn_whenAvailable_decrementsAndReturnsTrue() {
        // given
        final FreeTicket ticket = FreeTicket.initial();

        // when
        final boolean consumed = ticket.consumeOn(TODAY);

        // then
        assertThat(consumed).isTrue();
        assertThat(ticket.remainingOn(TODAY)).isZero();
    }

    @Test
    @DisplayName("같은 날 한도를 초과해 소비하면 false를 반환한다")
    void consumeOn_whenExhausted_returnsFalse() {
        // given
        final FreeTicket ticket = FreeTicket.initial();
        ticket.consumeOn(TODAY);

        // when & then
        assertThat(ticket.consumeOn(TODAY)).isFalse();
    }

    @Test
    @DisplayName("날이 바뀌면 잔여는 한도로 보이되 필드는 바뀌지 않는다(읽기 전용)")
    void remainingOn_whenNewDay_returnsLimitWithoutMutation() {
        // given
        final FreeTicket ticket = FreeTicket.initial();
        ticket.consumeOn(TODAY);

        // when
        final int remaining = ticket.remainingOn(TOMORROW);

        // then
        assertThat(remaining).isEqualTo(1);
        assertThat(ticket.getResetDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("날이 바뀌면 소비 시 리필 후 차감된다")
    void consumeOn_whenNewDay_refillsThenConsumes() {
        // given
        final FreeTicket ticket = FreeTicket.initial();
        ticket.consumeOn(TODAY);

        // when
        final boolean consumed = ticket.consumeOn(TOMORROW);

        // then
        assertThat(consumed).isTrue();
        assertThat(ticket.getResetDate()).isEqualTo(TOMORROW);
        assertThat(ticket.remainingOn(TOMORROW)).isZero();
    }
}
