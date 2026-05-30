package com.lingring.domain.matching.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.domain.MatchingPollStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchingStatusResponseTest {

    @Test
    @DisplayName("awaitingConfirm: partnerId와 confirmDeadline만 포함하고 roomId/callId는 null이다")
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
        assertThat(response.callId()).isNull();
    }

    @Test
    @DisplayName("matched: confirmDeadline은 null이고 partnerId/roomId/callId가 채워진다")
    void matched_populatesCallIdAndNullsDeadline() {
        // given
        final UUID roomId = UUID.randomUUID();
        final Long callId = 7L;

        // when
        final MatchingStatusResponse response = MatchingStatusResponse.matched(2L, roomId, callId);

        // then
        assertThat(response.status()).isEqualTo(MatchingPollStatus.MATCHED);
        assertThat(response.partnerId()).isEqualTo(2L);
        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(response.callId()).isEqualTo(callId);
        assertThat(response.confirmDeadline()).isNull();
    }

    @Test
    @DisplayName("waiting: 모든 보조 필드(partnerId/roomId/confirmDeadline/callId)는 null이다")
    void waiting_allOptionalFieldsAreNull() {
        // when
        final MatchingStatusResponse response = MatchingStatusResponse.waiting();

        // then
        assertThat(response.status()).isEqualTo(MatchingPollStatus.WAITING);
        assertThat(response.partnerId()).isNull();
        assertThat(response.roomId()).isNull();
        assertThat(response.confirmDeadline()).isNull();
        assertThat(response.callId()).isNull();
    }

    @Test
    @DisplayName("none: 모든 보조 필드(partnerId/roomId/confirmDeadline/callId)는 null이다")
    void none_allOptionalFieldsAreNull() {
        // when
        final MatchingStatusResponse response = MatchingStatusResponse.none();

        // then
        assertThat(response.status()).isEqualTo(MatchingPollStatus.NONE);
        assertThat(response.partnerId()).isNull();
        assertThat(response.roomId()).isNull();
        assertThat(response.confirmDeadline()).isNull();
        assertThat(response.callId()).isNull();
    }
}