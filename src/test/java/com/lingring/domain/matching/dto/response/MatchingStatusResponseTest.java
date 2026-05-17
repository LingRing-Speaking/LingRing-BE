package com.lingring.domain.matching.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.domain.MatchingPollStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchingStatusResponseTest {

    @Test
    @DisplayName("awaitingConfirm: partnerId와 confirmDeadline만 포함하고 roomId는 null이다")
    void awaitingConfirm_includesPartnerAndDeadlineOnly() {
        // given
        final LocalDateTime deadline = LocalDateTime.of(2026, 5, 12, 12, 0, 15);

        // when
        final MatchingStatusResponse response = MatchingStatusResponse.awaitingConfirm(2L, deadline);

        // then
        assertThat(response.status()).isEqualTo(MatchingPollStatus.AWAITING_CONFIRM);
        assertThat(response.partnerId()).isEqualTo(2L);
        assertThat(response.confirmDeadline()).isEqualTo(deadline);
        assertThat(response.roomId()).isNull();
    }

    @Test
    @DisplayName("matched: confirmDeadline은 null이다")
    void matched_nullDeadline() {
        // given
        final UUID roomId = UUID.randomUUID();

        // when
        final MatchingStatusResponse response = MatchingStatusResponse.matched(2L, roomId);

        // then
        assertThat(response.confirmDeadline()).isNull();
    }
}
