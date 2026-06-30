package com.lingring.domain.review.domain.quota;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaidTicketTest {

    @Test
    @DisplayName("빈 유료 티켓은 소비 시 false를 반환한다")
    void consume_whenEmpty_returnsFalse() {
        // given
        final PaidTicket ticket = PaidTicket.empty();

        // when & then
        assertThat(ticket.consume()).isFalse();
    }

    @Test
    @DisplayName("충전하면 잔여가 늘고 소비 시 1씩 줄며 true를 반환한다")
    void charge_thenConsume_decrementsAndReturnsTrue() {
        // given
        final PaidTicket ticket = PaidTicket.empty();
        ticket.charge(2);

        // when
        final boolean consumed = ticket.consume();

        // then
        assertThat(consumed).isTrue();
        assertThat(ticket.getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("잔여를 모두 소비하면 그 다음 소비는 false다")
    void consume_untilExhausted_thenFalse() {
        // given
        final PaidTicket ticket = PaidTicket.empty();
        ticket.charge(1);

        // when
        ticket.consume();

        // then
        assertThat(ticket.consume()).isFalse();
    }
}
